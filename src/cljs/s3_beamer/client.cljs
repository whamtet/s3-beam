(ns s3-beamer.client
  (:import [goog Uri]
           [goog.net XhrIo EventType ErrorCode]
           [goog.events EventType])
  (:require [goog.events :as events]
            [cljs.reader :refer [read-string]]
            )
  )

(defn url-from-map [url m]
  (let [uri (Uri. url)]
    (doseq [[k v] m]
      (.setParameterValue uri (name k) v))
    (.toString uri)))

(defn form-data-from-map [m]
  (let [fd (new js/FormData)]
    (doseq [[k v] m]
      (.append fd (name k) v))
    fd))

(defn upload-file [text file success-fn error-fn upload-listener xhr-handler]
  (let [
         params (read-string text)
         form-data (form-data-from-map (dissoc params :action))
         _ (.append form-data "file" file)
         options #js {:url (params :action)
                      :data form-data
                      :type "POST"
                      :contentType false
                      :processData false
                      :xhr (fn []
                             (let [xhr (js/$.ajaxSettings.xhr)]
                               (set! (-> xhr .-upload .-onprogress) upload-listener)
                               (set! (-> xhr .-upload .-onload) success-fn)
                               (set! (-> xhr .-upload .-onerror) error-fn)
                               (when xhr-handler (xhr-handler xhr))
                               xhr
                               ))}
         ]
    (js/$.ajax options)))

(defn upload [{:keys [server-url file success-fn error-fn upload-listener sign-params xhr-handler]}]
  (let [
         server-url (or server-url "/sign")
         xhr (XhrIo.)
         sign-params (merge {:file-name (.-name file) :mime-type (.-type file)} sign-params)
         ]
    (events/listen xhr goog.net.EventType.SUCCESS #(upload-file
                                                     (.getResponseText xhr)
                                                     file
                                                     success-fn
                                                     error-fn
                                                     upload-listener
                                                     xhr-handler))

    (when
      error-fn (events/listen xhr goog.net.EventType.ERROR error-fn))
    (.send xhr (url-from-map server-url sign-params) "GET")))

