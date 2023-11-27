(ns logical-english-client.le-api-client.pengines.core
  (:require ["pengines/pengines$default" :as Pengine]
            [applied-science.js-interop :as jsi]
            [cljs-bean.core :as bean]
            [lambdaisland.uri :as uri]
            [meander.epsilon :as m]
            [meander.strategy.epsilon :as r]
            [promesa.core :as prom]
            [tupelo.core :refer [it->]]))

(defn transform-pengine-result [pengine-result]
  (-> pengine-result
      (m/rewrite
       {:event (m/some "success") & ?rest}
       {:success true & ?rest}

       {:event (m/some (m/not "success")) & ?rest}
       {:success false & ?rest})

      (m/match
       {:data (m/some [?result & _])
        :success (m/some ?success)
        :more (m/some ?more)}
        {:answer ?result
         :success ?success
         :more-answers? ?more})))

(def ^:private pengine
  (atom nil))

(defn query-pengine! [server-url query]
  (let [result (prom/deferred)

        [hence! lest!]
        (for [f! [prom/resolve! prom/reject!]]
          #(this-as this
            (it-> this
              (bean/->clj it :keywordize-keys true)
              (f! result it))))

        re-init-pengine!
        (fn [old-pengine]
          (some-> old-pengine (jsi/call :stop))
          (Pengine.
           #js {:server (-> server-url (uri/join "/pengine") uri/uri-str)
                :ask query
                :onsuccess hence!
                :onfailure lest!}))]
    ;; We use a lock here because reinitializing the pengine object is
    ;; side-effecting.
    ;; See: https://stackoverflow.com/questions/41505946/what-is-the-correct-way-to-perform-side-effects-in-a-clojure-atom-swap
    (locking pengine
      (swap! pengine re-init-pengine!))
    (->> result (prom/map transform-pengine-result))))

(defn- query-le! [server-url file-name le-prog scenario-name query-name]
  (let [ans-var "Answer"

        extract-le-html-ans
        (r/rewrite {:answer {~(keyword ans-var) ?ans} & ?rest}
                   {:le-html-str ?ans & ?rest})

        le-query
        (str "le_answer:parse_and_query_and_explanation("
             file-name ", "
             "en(\"" le-prog "\")" ", "
             query-name ", "
             "with(" scenario-name ")" ", "
             ans-var
             ")")]
    (->> le-query
         (query-pengine! server-url)
         (prom/map extract-le-html-ans))))

(defn query-le-with-new-data!
  [server-url file-name le-prog & {:keys [query scenario]}]
  (let [n (reduce + (repeatedly (rand-int 5) #(rand-int 1000))) 
        scenario-name (if scenario (str "s" n) "")
        query-name (str "q" n)
        le-prog (str le-prog "\n"
                     (if scenario
                       (str "scenario " scenario-name " is:\n" scenario)
                       "")
                     "query " query-name " is:\n"
                     query)]
    (query-le! server-url file-name le-prog scenario-name query-name)))