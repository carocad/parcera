(ns test-cases)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x -9.78 "Hello, World!"))

;; TODO: is this a bug ?
(def foo.bar "hello")
;; TODO: is this a bug ?
(def . "hello")

;; TODO: is this a bug ?
#_(defn foo.bar [a.b]) ;; not valid ... why ?

;; TODO: is this a bug ? ;; a keyword starting with a number
;; :1hello.world

(meta ^{:a 1 :b 2} [1.2 2.3 0x12])

(meta ^String [1 2 3])

'(meta ^"String" [1 2 3])

#"hellow|wprld"

nil

true

#'clojure []

\-

(defn hello? [x] (if (nil? x) nil ::tap-nil x))

(str '(nil))

(set! *print-length* 30)

#_(def 9.7 "hello world")

'(#_(defn hello? [x] (if (nil? x) ::tap-nil x)))

{:a 1 :b "2" :c 'hello}
