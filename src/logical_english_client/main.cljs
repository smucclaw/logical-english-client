(ns logical-english-client.main
  (:require [cljs-bean.core :as bean]
            ;; [logical-english-client.le-api-client.natural4-server.core :as le-natural4-server-api]
            [logical-english-client.le-api-client.pengines.core :as le-pengines-api]
            [logical-english-client.le-api-client.swipl-wasm.core :as le-wasm-api]
            [logical-english-client.le-expln-to-json.core :as le-expln-to-json]
            [logical-english-client.webform-facts-to-le.core :as webform-facts-to-le]
            [promesa.core :as prom]
            [tupelo.core :refer [it->]]))

#_(defn query-le! [le-swish-server-url le-prog data le-query]
  (it-> data
        (webform-facts-to-le/data->le-scenario it)
        (le-natural4-server-api/query-le-with-new-data!
         le-swish-server-url le-prog {:scenario it :query le-query})
        (prom/chain' it :body le-expln-to-json/le-html-str->clj)))

(defn query-le!
  ([le-swish-server-url le-prog data le-query]
   (query-le! le-swish-server-url "test" le-prog data le-query))

  ([le-swish-server-url file-name le-prog data le-query]
   (it-> data
    (webform-facts-to-le/data->le-scenario it)
    (le-pengines-api/query-le-with-new-data!
     le-swish-server-url file-name le-prog {:scenario it :query le-query})
    (prom/chain' it
     :le-html-str
     le-expln-to-json/le-html-str->clj))))

(def query-le-js!
  (comp #(prom/handle % bean/->js) query-le!))

(defn re-init-le-swipl! [le-qlf-url]
  (le-wasm-api/re-init-le-swipl! le-qlf-url))

(defn query-le-wasm! [le-prog data le-query]
 (it-> data
  (webform-facts-to-le/data->le-scenario it)
  (le-wasm-api/query-le-with-new-data! le-prog it le-query)
  (prom/chain' it le-expln-to-json/le-html-str->clj)))

(def query-le-wasm-js!
  (comp #(prom/handle % bean/->js) query-le-wasm!))

(def render-le-resp-with-guifier!
  le-expln-to-json/render-le-resp-with-guifier!)

(def le-tree->goals-js
  le-expln-to-json/le-tree->goals-js)

(def le-tree->str
  le-expln-to-json/le-tree->str)

(def clj-to-js
  bean/->js)

(def js-to-clj
  bean/->clj)