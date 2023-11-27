(ns le-webform-glue.le-expln-to-json.core
  (:require ["guifier$default" :as Guifier]
            [applied-science.js-interop :as jsi]
            [cljs-bean.core :as bean]
            [le-webform-glue.utils :as utils]
            [meander.epsilon :as m]
            [meander.strategy.epsilon :as r]
            [taipei-404.html :refer [html->hiccup]]
            [tupelo.string :as str]))

;; (defrecord Goal [pos-or-neg goal-str child-goals])

;; Small step evaluation semantics for transforming LE html trees into
;; a nicer form.

;; Evaluation contexts:
;; E ::= [.] | <?tag ... > E ... E </ ?tag>
;; Note that metavariables are prefixed with ?
;; This has the same effect as extending the small step rewriting judgement
;; form (axiomatized below) to a catamorphism over the html tree.

;; ------------------------------
;; <html>
;; <head> _ </head>
;; <body> ?body </body> => ?body
;; </html>

;; -----------------------------------------------------
;; <li title = "Rule inference" >
;;   ?x                           => [:proved? true] ?x
;; </li>

;; ------------------------------------------------------
;; <li title = "Failed goal" >
;;   ?x                           => [:proved? false] ?x
;; </li>

;; ------------------------
;; <b> ?goal </b> => ?goal

;; -----------------------------------------------
;; <span class= "leaf" > </span> => [:because []]

;; ------------------------------------------------------------------------------------------
;; <ul class= "nested" > because
;;   ?child-goal-0 and ... and ?child-goal-n  => [:because [?child-goal-0 ... ?child-goal-n]]
;; </ul>

;; -------------------------------------------
;; [k0 v0] ... [kn vn] => {k0 v0, ..., kn vn}

(def hiccup->map
  (r/bottom-up
   (r/rewrite
    ;; [:html {} [:head {}] [:body {} ?body & _]] ?body
    
    [:li . (m/or [KEEP & !kv-pairs] _) ...] {& [!kv-pairs ...]}

    {:title "Rule inference"} [KEEP :proved? true]
    {:title "Failed goal"} [KEEP :proved? false]

    [:b ?goal] [KEEP :goal ?goal]

    ;; [:span {:class "leaf"} " "] [KEEP :because []]
    
    [:ul {:class "nested"} _because . !child-goal _sep ...]
    [KEEP :because [!child-goal ...]]

    ?x ?x)))

(defn- num-or-str->at-most-num-dp-str
  "Hacky way to parse strings representing doubles into a number of <= num decimal
   places. Input can either be a number or a string representing a number.
   Note that \"123.000\" becomes 123.0."
  [num-dp num]
  (let [num->at-most-num-dp-str
        #(-> % (jsi/call :toFixed num-dp) parse-double str)]
    (cond
      (number? num) (num->at-most-num-dp-str num)
      (string? num) (-> num parse-double num->at-most-num-dp-str))))

;; (jsi/call js/console :log
;;           "Double: "
;;           (double-str->at-most-num-dp-str 3 "123.020"))

(defn- cleanup-goal-str [goal-str]
  (utils/replace-strs
   goal-str
   [["  s" "'s"]
    ;; ["you's" "your"]
    ;; ["You's" "Your"]
    ;; ["you qualifies" "you qualify"]
    ;; ["you has" "you have"]
    [#"\d+\.\d\d+" (partial num-or-str->at-most-num-dp-str 3)]
    ;; [#"(\d+)\.0+" "$1"]
    [#"date\(\s*(\d+),\s*(\d+),\s*(\d+)\)" "$1-$2-$3"]
    [" COMMA" ","]
    ;; #"\d+ PERCENT" #(str/replace % " PERCENT" "%") 
    [" PERCENT" "%"]
    [" PERIOD " "."]
    ["&lt;" "<"]
    [#"\s+inf[^a-zA-Z]+" " infinity"]
    [#"[^a-zA-Z]+inf\s+" "infinity "]
    [#"\*(a\s.*)\*" "$1"]]))

(def cleanup-goal-strs
  (r/top-down
   (r/rewrite
    {:goal (m/some ?goal) & ?rest}
    {:goal ~(cleanup-goal-str ?goal) & ?rest}

    ?x ?x)))

(def remove-prolog-metadata-nodes
  (r/pipe
   (r/top-down
    (r/rewrite
     {:because (m/some ?child-goals) & ?rest}
     {:because (map aux over ?child-goals and then flatten-1) & ?rest}

     ?x ?x))

   ;; Fixpoint iteration to compute the (full) head normal form of a tree
   ;; (which exists as strong normalization and confluence are obvious)
   (r/fix
    (r/bottom-up
     (r/rewrite
      (map ?map-fn over [!xs ...] and then ?combine-fn)
      (?combine-fn [(?map-fn !xs) ...])

      (aux {:goal (m/re #"^.*Prolog.*$") :because ?child-goals})
      ?child-goals

      (aux (m/and ?child-goal {:goal (m/not (m/re #"^.*Prolog.*$"))}))
      ?child-goal

      (flatten-1 []) []
      (flatten-1 [(m/seqable ?xs) & ?rest]) [& ?xs & ?rest]
      (flatten-1 [(m/and ?x (m/not m/seqable)) & ?rest]) [?x & ?rest]

      ?x ?x)))))

(def transform-negs
  (let [split-str-on-first-neg #(str/split % #"it is not the case that " 2)]
    (r/top-down
     (r/rewrite
      ;; The first 2 rules deal with eliminating double negations in the
      ;; explanation tree, while the 3rd deals with flipping positive goals of
      ;; negations into negative goals.
      {:proved? false
       :goal (m/app split-str-on-first-neg ["" _])
       :because (m/some [?pos-goal])}
      ?pos-goal

      {:proved? false
       :goal (m/app split-str-on-first-neg ["" ?goal])
       :because nil}
      {:proved? true :goal ?goal}

      {:proved? true
       :goal (m/app split-str-on-first-neg ["" ?goal])
       :because ?because}
      {:proved? false
       :goal ?goal
       :because ?because}

      ?x ?x))))

(def post-process-keys
  (r/top-down
   (r/rewrite
    {:proved? ?proved :goal ?goal :because ?because}
    {:true? ?proved :text ?goal :because ?because}

    ?x ?x)))

(def post-process-le-expln-map
  (r/pipe cleanup-goal-strs
          ;; remove-prolog-metadata-nodes
          transform-negs
          post-process-keys))

(defn le-html-str->clj [le-html-str]
  (-> le-html-str
      str/trim
      html->hiccup
      first
      hiccup->map
      post-process-le-expln-map))

(def transform-map-for-guifier
  (r/pipe
   (r/top-down
    (r/rewrite
     {:text ?text :true? false :because ?because}
     {~(str "❌ " ?text) ?because}

     {:text ?text :true? true :because ?because}
     {~(str "✓ " ?text) ?because}

     ?x ?x))

  ;;  (r/top-down
  ;;   (r/rewrite
  ;;    {~(str "it is not the case that " ?goal) ?because}
  ;;    {~(str "❌ " ?goal) ?because}

  ;;    {?goal ?because}
  ;;    {~(str "✓ " ?goal) ?because}

  ;;    ?x ?x))

   ;; m/cata(morphism) over input seqs of goals to inline goal strings
   ;; in the seq, so they show up without an extra layer of nesting. 
   (r/bottom-up
    (r/rewrite
     [{?goal (m/some ?because)} & (m/cata ?rest)] [?goal ?because & ?rest]
     [{?goal (m/not m/some)} & (m/cata ?rest)] [?goal & ?rest]
     ?x ?x))))

(defn render-le-resp-with-guifier! [guifier-elem-id le-resp]
  (-> le-resp
      bean/->clj
      transform-map-for-guifier
      bean/->js
      (#(Guifier.
         #js {:data % :dataType "js"
              :elementSelector (str "#" guifier-elem-id)
              :withoutContainer true
              :readOnlyMode true})))
  (doseq [elem (jsi/call js/document
                         :getElementsByClassName "guifierFieldLabelName")]
    (jsi/update! elem :innerHTML
                 #(str/replace % "Array" "Explanation"))))

;; (defn le-html-str->js [le-html-str]
;;   (-> le-html-str le-html-str->clj bean/->js))

(defn- le-tree->goals [le-tree]
  (-> le-tree
      bean/->clj
      (m/breadth-first-search
       (m/$ {:text ?text :true? ?true :because _})
       {:text ?text :true? ?true})))

(defn le-tree->goals-js [le-tree]
  (-> le-tree le-tree->goals bean/->js))

(defn- goals->str [goals]
  (let [goal->str
        (r/match
         {:text ?text :true? (m/pred boolean? ?b)}
          (str (-> ?b str str/upper-case) " " ?text)

          _ nil)

        xf-goals->str
        (comp (map goal->str)
              (filter some?)
              (interpose "\n\n"))]
    (->> goals (eduction xf-goals->str) str str/trim)))

(defn le-tree->str [le-tree]
  (-> le-tree le-tree->goals goals->str))