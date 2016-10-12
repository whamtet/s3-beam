(defproject whamtet/s3-beamer "0.6.0-SNAPSHOT"
  :description "CORS Upload to S3 via Clojure(script)"
  :url "http://github.com/whamtet/s3-beamer"

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]
                 ;[com.google.javascript/closure-compiler "v20140814"]
                 [org.clojure/data.json "0.2.5"]
                 [ring/ring-codec "1.0.0"]]

  :scm {:name "git"
         :url "https://github.com/whamtet/s3-beamer"})
