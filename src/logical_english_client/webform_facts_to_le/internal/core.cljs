(ns logical-english-client.webform-facts-to-le.internal.core
  (:require [logical-english-client.webform-facts-to-le.internal.asami :as asami]
            [logical-english-client.webform-facts-to-le.internal.date :as date]
            [logical-english-client.webform-facts-to-le.internal.datom :as datom]
            [malli.core :as malli]
            [meander.epsilon :as m]))

(malli/=> asami-triple->le-datom [:=> [:cat asami/triple] datom/le-datom])
(defn- asami-triple->le-datom [asami-triple]
  (m/match asami-triple
    (m/seqable ?e :a/contains ?v)
    (datom/MembershipDatom. ?v ?e)

    (m/seqable ?e ?a (m/app date/str->maybe-date (m/some ?date)))
    (datom/DateDatom. ?e ?a ?date)

    (m/seqable ?e ?a ?v)
    (datom/GenericDatom. ?e ?a ?v)))

(malli/=> asami-triples->le-datoms [:=> [:cat [:sequential asami/triple]] [:sequential datom/le-datom]])
(defn- asami-triples->le-datoms [asami-triples]
  (eduction (map asami-triple->le-datom) asami-triples))

(malli/=> clj-data->le-scenario [:=> [:cat :any] :string])
(defn clj-data->le-scenario [clj-data]
  (-> clj-data
      asami/clj-data->triples
      asami-triples->le-datoms
      datom/le-datoms->le-scenario))