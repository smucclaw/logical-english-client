(ns logical-english-client.le-api-client.natural4-server.core 
  (:require [lambdaisland.fetch :as fetch]))

(defn query-le!
  [le-server-url le-prog scenario-name query-name]
  (fetch/post le-server-url
              {:accept :html
               :content-type :json
               :body {:le_prog le-prog
                      :scenario_name scenario-name
                      :query_name query-name}}))

(defn query-le-with-new-data!
  [server-url le-prog & {:keys [query scenario]}]
  (let [n (reduce + (repeatedly (rand-int 5) #(rand-int 1000))) 
        scenario-name (if scenario (str "s" n) "")
        query-name (str "q" n)
        le-prog (str le-prog "\n"
                     (if scenario
                       (str "scenario " scenario-name " is:\n" scenario)
                       "")
                     "query " query-name " is:\n"
                     query)]
    (query-le! server-url le-prog scenario-name query-name)))