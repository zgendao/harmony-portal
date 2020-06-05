(ns app.request
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<!]]
            ;[cljs.reader :as reader :refer [read-string]]
            [cljs-http.client :as http]
            [app.data :refer [dat]]
            [app.storage :refer [app-state]]))

(def const (atom @dat))
(def data (atom []))
(def all-validator (atom {}))

(defn request []
  (go (let [];all-information-response (<! (http/get "http://staking.hmny.io:8090/networks/harmony/validators" {:with-credentials? false :headers {"Content-Type" "application/json"}}))
            ;validators ((all-information-response :body) :validators)]
        (doseq [[id validator] (map-indexed vector @const)]
          (let [address (validator :address)
                delegations (validator :delegations)
                shift 0.000000000000000001
                total-stake (* shift (validator :total_stake))
                self-stake (* shift (validator :self_stake))]
            (do
              (swap! all-validator assoc-in [:reviews (keyword (:address validator))] {})
              (swap! data conj (assoc {}
                                      :id id
                                      :address address
                                      :delegations delegations
                                      :total-stake (* shift (validator :total_stake))
                                      :self-stake (* shift (validator :self_stake))
                                      :fee (validator :rate)
                                      :max-delegation (* shift (validator :max-total-delegation))
                                      :total-delegation (- total-stake self-stake)
                                      :name (validator :name)
                                      :description (validator :details)
                                      :website (validator :website)
                                      :committee (validator :currently-in-committee)
                                      :active (validator :active)
                                      :return (validator :apr)
                                      :lifetime-reward (validator :lifetime_reward_accumulated)
                                      :uptime (validator :uptime_percentage)
                                      :height (validator :creation-height)))))))))

(defn fetch-validators [])
  ;(reset (:reviews app-state) (merge (:reviews @app-state) (:reviews @all-validator))))
