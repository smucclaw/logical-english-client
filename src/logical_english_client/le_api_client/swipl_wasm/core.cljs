(ns logical-english-client.le-api-client.swipl-wasm.core
  (:require [applied-science.js-interop :as jsi]
            [cljs-bean.core :as bean]
            [promesa.core :as prom]
            [shadow.esm :refer [dynamic-import]]))

(def ^:private swipl-wasm-cdn-url
  "https://SWI-Prolog.github.io/npm-swipl-wasm/3/5/13/dynamic-import.js")

(def ^:private le-swipl
  (atom nil))

(defn re-init-le-swipl! [le-qlf-url]
  (prom/let [^js swipl-mod (dynamic-import swipl-wasm-cdn-url)
             swipl (-> swipl-mod
                       (jsi/get :SWIPL)
                       (new (bean/->js {:arguments ["-q"]})))

            ;; Ugly hack to get swipl wasm working on nodejs.
            ;; The issue is that it fails to load prolog and qlf files on nodejs via Prolog.consult
            ;; with following error:
            ;; ERROR: JavaScript: ReferenceError: window is not defined
            ;; To solve this, we assign a global window object to an empty object just so
            ;; that it's defined.
             _ (when-not (or (exists? js/window)
                             (jsi/get js/globalThis :window))
                 (jsi/assoc! js/globalThis :window #js {}))

             _ (-> swipl
                   (jsi/get :prolog)
                   (jsi/call :consult le-qlf-url))]
    (jsi/call js/console :log "Loaded Swipl Mod: " swipl-mod)
    (jsi/call js/console :log "SWIPL: " swipl)
    (reset! le-swipl swipl)))

;; (def ^:private result
;;   (prom/chain'
;;    le-swipl
;;    #(jsi/get % :prolog)
;;    #(jsi/call % :query "declarative_date_time:is_duration_before_after_within(today, days(180), within, today).")
;;    #(jsi/call % :once)))

;; (prom/let [result result]
;;   (jsi/call js/console :log "Query result: " result))

;; (def ^:private le-prog
;; "the target language is: prolog.

;; the templates are:
;;  *a person* is the parent of *a person*. 
;;  *a person* is the grandparent of *a person*.

;; the knowledge base rules includes:
;;   a person is the grandparent of an other person
;;   if the person is the parent of a third person
;;   and the third person is the parent of the other person.

;; scenario s is:
;;   alice is the parent of bob.
;;   bob is the parent of charlie.

;; query q is:
;;   which person is the grandparent of which other person.
;; ")

;; (def ^:private le-query "q")

;; (def ^:private swipl-query
;;   (str "le_answer:parse_and_query_and_explanation(\"test\", en(LE_prog), LE_query, with(Scenario), JustificationHtml)."))

;; (def ^:private swipl-query-params
;;   #js {:LE_prog le-prog
;;        :LE_query le-query
;;        :Scenario "s"})

;; (def ^:private first-soln
;;   (prom/chain' le-swipl
;;                #(-> % (jsi/get :prolog) (jsi/call :query swipl-query swipl-query-params))
;;                #(jsi/call % :once)
;;                #(jsi/get-in % [:JustificationHtml :v])))

(defn- query-le! [le-prog scenario-name query-name]
  (let [swipl-query-str
        "le_answer:parse_en_and_query_and_explanation(
           LE_prog, LE_query, LE_scenario, JustificationHtml
        )"

        swipl-query-params
        #js {:LE_prog le-prog
             :LE_query query-name
             :LE_scenario scenario-name}]

    (jsi/call js/console :log "SWIPL query params:\n" swipl-query-params)
    (prom/chain'
     @le-swipl
     #(jsi/get % :prolog)
     #(jsi/call % :query swipl-query-str swipl-query-params)
     #(jsi/call % :once)
     #(jsi/get-in % [:JustificationHtml :v]))))

(defn query-le-with-new-data!
  [le-prog scenario query]
  (let [n (reduce + (repeatedly (rand-int 5) #(rand-int 1000))) 
        scenario-name (if scenario (str "s" n) "")
        query-name (str "q" n)
        le-prog (str le-prog "\n"
                     (if scenario
                       (str "scenario " scenario-name " is:\n" scenario)
                       "")
                     "query " query-name " is:\n"
                     query)]
    (query-le! le-prog scenario-name query-name)))

;; (prom/let [first-soln (query-le! le-prog "s" "q")]
;;   (js/console.log "First solution: " first-soln))