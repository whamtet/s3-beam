(ns s3-beamer.handler
  (:require [clojure.data.json :as json]
            [ring.util.codec :refer [base64-encode]])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)
           (java.text SimpleDateFormat)
           (java.util Date TimeZone)))

(defn now-plus [n]
  "Returns current time plus `n` minutes as string"
  (let [f (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
    (.setTimeZone f (TimeZone/getTimeZone "UTC" ))
    (.format f (Date. (+ (System/currentTimeMillis) (* n 60 1000))))))

(defn policy
  "Generate policy for upload of `key` with `mime-type` to be uploaded
  within `expiration-window`, and `acl`."
  [bucket key mime-type expiration-window acl x-amz-credential x-amz-algorithm x-amz-date]
  (let [policy (json/write-str { "expiration" (now-plus expiration-window)
                                 "conditions" [{"bucket" bucket}
                                               {"acl" acl}
                                               ["starts-with" "$Content-Type" mime-type]
                                               ["starts-with" "$key" key]
                                               {"x-amz-credential" x-amz-credential}
                                               {"x-amz-algorithm" x-amz-algorithm}
                                               {"x-amz-date" x-amz-date}
                                               {"success_action_status" "201"}]})
        ]
    ;(println "got policy" policy)
    (ring.util.codec/base64-encode
      (.getBytes policy
                 "UTF-8"))))

(defn hmac-sha256 [k s]
  (.doFinal (doto (Mac/getInstance "HmacSHA256") (.init (SecretKeySpec. k "HmacSHA256"))) (.getBytes s "UTF-8")))

(defn hex [s]
  (apply str (map
               (fn [s]
                 (let [
                        hex (Integer/toHexString s)
                        ]
                   (condp = (.length hex) 1 (str 0 hex) 8 (.substring hex 6) hex))) s)))

(defn yyyyMMdd []
  (.format (SimpleDateFormat. "yyyyMMdd") (Date.)))
(defn x-amz-date []
  (.format (SimpleDateFormat. "yyyyMMdd'T000000Z'") (Date.)))

(defn sign-version-4
  ([k s region]
   (-> (str "AWS4" k)
       (.getBytes "UTF-8")
       (hmac-sha256 (yyyyMMdd))
       (hmac-sha256 region)
       (hmac-sha256 "s3")
       (hmac-sha256 "aws4_request")
       (hmac-sha256 s)
       hex
       )))

(defn sign-upload [{:keys [file-name mime-type]}
                   {:keys [bucket aws-zone aws-access-key aws-secret-key acl upload-url key accelerate?] :or {acl "private"}}]
  (assert aws-access-key "AWS Access Key cannot be nil")
  (assert aws-secret-key "AWS Secret Key cannot be nil")
  (assert acl "ACL cannot be nil")
  (assert mime-type "Mime-type cannot be nil.")
  (let [yyyyMMdd (yyyyMMdd)
        key (or key file-name)
        x-amz-credential (apply str (interpose "/" [aws-access-key yyyyMMdd aws-zone "s3" "aws4_request"]))
        x-amz-algorithm "AWS4-HMAC-SHA256"
        x-amz-date (x-amz-date)
        p (policy bucket key mime-type 60 acl x-amz-credential x-amz-algorithm x-amz-date)
        accelerate? (if accelerate? "-accelerate" "")
        ]
    {:action (or upload-url (format "https://%s.s3%s.amazonaws.com" bucket accelerate?))
     :key    key
     :Content-Type mime-type
     :policy p
     :acl    acl
     :success_action_status "201"
     :x-amz-credential x-amz-credential
     :x-amz-algorithm x-amz-algorithm
     :x-amz-date x-amz-date
     :x-amz-signature (sign-version-4 aws-secret-key p aws-zone)}))

(defn s3-sign [a b]
  {:status 200
   :body (pr-str (sign-upload a b))})
