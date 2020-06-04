(ns harmonyPortal.app
  (:require
   [reagent.core :as r]
   [harmonyPortal.views :refer [portal]]
   [harmonyPortal.storage :refer [app-state]]
   [harmonyPortal.request :refer [request]]
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
          [u r])))

(defn make-bide-router [rmap]
  (b/router (transform-map rmap)))

(def route-map {:harmonyPortal.home "/home"
                :harmonyPortal.about "/about"})

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
;     [:div [nav-link :harmonyPortal.home "Homee"]]
;     [:div [nav-link :harmonyPortal.about "About"]]]
;    (case name
;      :harmonyPortal.home (home-page app-state)
;      :harmonyPortal.about [about-page])])

(defn on-navigate
  "A function which will be called on each route change."
  [name params query app-state]
  (r/render [portal app-state]
    (.getElementById js/document "app")))

(defn ^:export run [app-state]
  (b/start! router {:default :harmonyPortal.home
                    :html5? true
                    :on-navigate (fn [name params query] (on-navigate name params query app-state))})
  (request))

(run app-state)
