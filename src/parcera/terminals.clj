(ns parcera.terminals)

;; Clojure's reader is quite permissive so we follow the motto "if it is not forbidden, it is allowed"
(def NAME "[^\\s\\(\\)\\[\\]{}\"@~\\^;`\\\\]+")
; todo: (?!\/) do i need that ?
;; symbols cannot start with a number, :, # nor '
(def symbol-pattern (str "(?![:#\\'])(" NAME "\\/)?(\\/|(" NAME "))"))


(def double-suffix "(((\\.\\d*)?([eE][-+]?\\d+)?)M?)")
; todo: (0)|([1-9]\d*) is this needed ?
(def long-suffix "((0[xX]([\\dA-Fa-f]+)|0([0-7]+)|([1-9]\\d?)[rR]([\\d\\w]+)|0\\d+)?N?)")
(def ratio-suffix "(\\/(\\d+))")
(def number-pattern (str "[+-]?\\d+(" long-suffix "|" double-suffix "|" ratio-suffix ")(?![\\.\\/])"))


(def unicode-char "(\\P{M}\\p{M}*+)") ;; https://www.regular-expressions.info/unicode.html
(def named-char "(newline|return|space|tab|formfeed|backspace)")
(def unicode "(u[\\dD-Fd-f]{4})")
(def character-pattern (str "\\\\(" unicode-char "|" named-char "|" unicode ")(?!\\w+)"))


; : is not allowed as first keyword character
(def simple-keyword (str ":(?!:)" symbol-pattern))
(def macro-keyword (str "::(?!:)" NAME))


(def string-pattern "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"")
(def regex-pattern (str "#" string-pattern))
