(ns test-beam.core
  (:require [s3-beamer.client :as s3])
  )

(enable-console-print!)

(defn file-changed []
  (let [
         file (-> "file" js/document.getElementById .-files array-seq first)
         ]
    (println "about to add file" file)
    (s3/upload {:file file
                :success-fn #(println "success")
                :upload-listener #(println "uploading")
                :sign-params {"hi" "there"}
                })))

(.on (js/$ "#file") "change" file-changed)
