(ns logical-english-client.webform-facts-to-le.internal.date 
  (:require [tick.core :as tick]
            [tupelo.string :as str]))

(defn ->maybe-date [s]
  (try
    (tick/date s)
    (catch js/Object _ nil)))

(defn date->le-date-str [date]
  (let [js-date->year-month-day-triple
        (juxt (comp tick/int tick/year)
              (comp tick/int tick/month)
              tick/day-of-month)]
    (->> date js-date->year-month-day-triple (str/join "-"))))

(defn today-as-le-date-str! []
  (date->le-date-str (tick/today)))