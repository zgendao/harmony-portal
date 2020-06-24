(ns app.request
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [app.storage :refer [app-state local state]]))

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
            (do (swap! data assoc address {:delegations (validator :delegations)
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
                                           :height (validator :creation-height)})
                (when-not (get-in @app-state [address :description]) (swap! app-state assoc-in [address :description] (validator :details)))))))))

(defn fetch-validators []
  (go (let [response (<! (http/get "https://staking-explorer2-268108.appspot.com/networks/harmony/validators" {:with-credentials? false :headers {"Content-Type" "application/json"}}))
            addresses (into {} (map (fn [v] {(keyword (v :address)) {}}) ((response :body) :validators)))]
        (reset! app-state (merge addresses @app-state)))))
