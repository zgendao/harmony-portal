(ns harmonyPortal.routes
  (:require
   [compojure.core :refer [routes GET ANY]]
   [compojure.route :as route]
   [hiccup.core :as hiccup]
   [ring.util.http-response :as response]))

(defn app [ring-req]
  (hiccup/html
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:href "/css/style.css" :rel "stylesheet" :type "text/css"}]
    [:link {:rel "icon" :href "/images/favicon.ico" :type "image/x-icon"}]
    ;[:link {:href "/css/blueprint.min.css" :type "text/html" :rel "stylesheet"}]
    [:title "Harmony Staking Community"]]
   [:body
    [:div#app]
    [:script {:src "js/app.js"}]]))

(defn site [_]
  (routes
   (GET "*" ring-req (app ring-req))
   (ANY "*" [] (hiccup/html [:h1 {:style {:text-align "center" :color "red"}} "NOT FOUND"]))))
