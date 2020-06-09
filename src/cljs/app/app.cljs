(ns app.app
  (:require
   [reagent.core :as r]
   [app.views :refer [portal]]
   [app.storage :refer [app-state]]
   [app.request :refer [get-validators fetch-validators get-accounts]]
   [bide.core :as b]))

(enable-console-print!)

; (defn home-page [app-state]
;   [:div "Home"
;    [:p (str @app-state)]
;    [:a {:on-click #(swap! app-state assoc "counter" (rand-int 1000))} "Assoc"]])
;
; (defn about-page []
;   [:div "About"])

(defn transform-map [rmap]
  (into []
        (for [[r u] rmap]
          [u r]))) ""
active-filte

(defn make-bide-router [rmap]
  (b/router (transform-map rmap)))

(def route-map {:app.home "/home"
                :app.about "/about"})

(def router (make-bide-router route-map))

; (defn nav-link [route text]
;   [:a {:href (route route-map)
;        :on-click #(do
;                     (-> % .preventDefault)
;                     (b/navigate! router route))} text])
;
; (defn page [name app-state]
;   [:div
;    [:nav
;     [:div [nav-link :app.home "Homee"]]
;     [:div [nav-link :app.about "About"]]]
;    (case name
;      :app.home (home-page app-state)
;      :app.about [about-page])])

(defn on-navigate
  "A function which will be called on each route change."
  [name params query app-state]
  (r/render [portal app-state]
    (.getElementById js/document "app")))

(defn ^:export run [app-state]
  (b/start! router {:default :app.home
                    :html5? true
                    :on-navigate (fn [name params query] (on-navigate name params query app-state))})
  (get-validators)
  (get-accounts)
  (fetch-validators))

(run app-state)
