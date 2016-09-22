(ns test-beam.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [ring.util.response :as response]
            [s3-beamer.handler :as s3b]
            [test-beam.build :as build]
            [environ.core :refer [env]]))

(def bucket "dicom-files")
(def aws-zone "ap-northeast-2")
(def access-key (System/getenv "S3_UPLOADER_KEY"))
(def secret-key (System/getenv "S3_UPLOADER_SECRET"))

(defroutes app
  (GET "/" [] (response/redirect "/test.html"))
  (GET "/sign" [file-name mime-type hi]
       (println "hi" hi)
       (s3b/s3-sign
         {:file-name file-name :mime-type mime-type}
         ;[bucket aws-zone aws-access-key aws-secret-key acl upload-url key]
         {:bucket bucket :aws-zone aws-zone :aws-access-key access-key
          :aws-secret-key secret-key :key (str "uploads/" file-name)}))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn wrap-app [app]
  ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
  (let [store (cookie/cookie-store {:key (env :session-secret)})]
    (-> app
        ((if (env :production)
           wrap-error-page
           trace/wrap-stacktrace))
        (site {:session {:store store}}))))

(defn -main [& [port]]
  (build/-main)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (wrap-app #'app) {:port port :join? false})))

;; For interactive development:
;(.stop server)
;(def server (-main))
