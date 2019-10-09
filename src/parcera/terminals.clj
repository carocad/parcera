(ns parcera.terminals)

;; Clojure's reader is quite permissive so we follow the motto "if it is not forbidden, it is allowed"
(def NAME "[^\\s\\(\\)\\[\\]{}\"@~\\^;`]+")
; todo: (?!\/) do i need that ?
;; symbols cannot start with a number, :, # nor '
(def SYMBOL (str "(?![:#\\'])(" NAME "\\/)?(\\/|(" NAME "))"))


(def DOUBLE "(((\\.\\d*)?([eE][-+]?\\d+)?)M?)")
; todo: (0)|([1-9]\d*) is this needed ?
(def LONG "((0[xX]([\\dA-Fa-f]+)|0([0-7]+)|([1-9]\\d?)[rR]([\\d\\w]+)|0\\d+)?N?)")
(def RATIO "(\\/(\\d+))")
(def NUMBER (str "[+-]?\\d+(" LONG "|" DOUBLE "|" RATIO ")(?![\\.\\/])"))

(def unicode-char "(\\P{M}\\p{M}*+)") ;; https://www.regular-expressions.info/unicode.html
(def named-char "(newline|return|space|tab|formfeed|backspace)")
(def unicode "(u[\\dD-Fd-f]{4})")
(def CHARACTER (str "\\\\" unicode-char "|" named-char "|" unicode))

; : is not allowed as first keyword character
(def SIMPLE-KEYWORD (str ":(?!:)" SYMBOL))
(def MACRO-KEYWORD (str "::(?!:)" NAME))
