(ns app.request
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [app.data :refer [dat]]
            [app.storage :refer [app-state local]]
            [brave.swords :as x]))

(def const (atom @dat))
(def data (atom {}))

; https://staking-explorer2-268108.appspot.com/networks/harmony/validators
; http://staking.hmny.io:8090/networks/harmony/validators

(defn get-validators []
  (go (let [response (<! (http/get "https://staking-explorer2-268108.appspot.com/networks/harmony/validators" {:with-credentials? false :headers {"Content-Type" "application/json"}}))
            validators ((response :body) :validators)]
        (doseq [validator validators]
          (let [address (keyword (validator :address))
                shift 0.000000000000000001
                total-stake (* shift (validator :total_stake))
                self-stake (* shift (validator :self_stake))]
            (swap! data assoc address {:delegations (validator :delegations)
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
                                       :height (validator :creation-height)}))))))

(defn fetch-validators []
  (go (let [response (<! (http/get "https://staking-explorer2-268108.appspot.com/networks/harmony/validators" {:with-credentials? false :headers {"Content-Type" "application/json"}}))
            addresses (into {} (map (fn [v] {(keyword (v :address)) {}}) ((response :body) :validators)))]
        (swap! app-state assoc :reviews (merge addresses (:reviews @app-state))))))

(defn get-accounts []
  (swap! local assoc-in [:own-validator :address] "one1r3kwetfy3ekfah75qaedwlc72npqm2gkayn6ue"))
  ; (.addEventListener js/window "message" #(println (str (x/obj->clj %))))
  ; (.postMessage js/window
  ;               (clj->js {:type "FROM_HARMONY_IO", :payload {:type "GET_WALLETS"}, :skipResponse false})
  ;               "*"))
