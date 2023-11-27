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

#_(defn- f [le-scenario data n]
  (let [dob
        (m/find data
                (m/$ {:dob (m/some ?date)})
                (date/str->maybe-date ?date))

        nth-birth-date
        (tick/>> dob (tick/new-period n :years))

        policy-start-date
        (m/find data
                (m/$ {:risk_commencement_date (m/some ?date)})
                (date/str->maybe-date ?date))

        num-years (tick/between policy-start-date nth-birth-date :years)

        last-policy-anniversary
        (tick/>> policy-start-date (tick/new-period num-years :years))

        date
        (m/find data
                (m/or (m/$ {:date_of_accident (m/some ?date)})
                      (m/$ {:date_of_diagnosis (m/some ?date)}))
                (date/str->maybe-date ?date))
        
        in-same-year
        (= 0 (tick/between last-policy-anniversary date :years))]

    ;; (js/console.log "Stuff: " in-same-year)
    (if in-same-year
      (str le-scenario
           "\n"
           "a life assured turns " n " in one policy year of the accident or diagnosis.")
      le-scenario)))

(malli/=> clj-data->le-scenario [:=> [:cat :any] :string])
(defn clj-data->le-scenario [clj-data]
  (-> clj-data
      asami/clj-data->triples
      asami-triples->le-datoms
      datom/le-datoms->le-scenario

      ;; Generate the following if applicable:
      ;; "a life assured turns 65 in one policy year of the accident or diagnosis.")
      ;; "a life assured turns 75 in one policy year of the accident or diagnosis.")
      ;; (f clj-data 65)
      ;; (f clj-data 75)
      ))