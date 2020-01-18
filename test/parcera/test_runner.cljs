(ns parcera.test-runner
  (:require [figwheel.main.testing :refer-macros [run-tests]]
    ;; require all the namespaces that have tests in them
            [parcera.test.core :as ct]))

(defn -main []
  (run-tests 'parcera.test.core))

;(set! *main-cli-fn* -main)
