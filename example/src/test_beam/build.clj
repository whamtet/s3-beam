(ns test-beam.build)

(require '[cljs.build.api :as b])
(import java.awt.Toolkit)


(defn -main [& args]
  (let [start (System/nanoTime)]
    (b/build "src-cljs"
             {:output-to "resources/public/test_beam.js"
              :output-dir "resources/public/release"
              :main "test_beam.core"
              :asset-path "release"
              :optimizations :none
              :verbose true})
    (println "... done. Elapsed" (/ (- (System/nanoTime) start) 1e9) "seconds")))
