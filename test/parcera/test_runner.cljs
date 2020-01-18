(ns parcera.test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [figwheel.main.testing :as tester]
    ;; require all the namespaces that have tests in them
            [parcera.test.core :as ct]))

(defn -main []
  (tester/run-tests 'parcera.test.core))

;(set! *main-cli-fn* -main)
