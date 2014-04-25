(ns om-snake.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [goog.events :as events]
            [cljs.core.async :as async :refer [>! <! put! chan timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(def height 40)
(def width 40)
(def size 10)

(def keycodes {37 :left 38 :up 39 :right 40 :down})

(def app-state (atom {:food #{[20 20]}
                      :snake [[10 20]]
                      :direction :up}))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type #(put! out %))
    out))

(defn render-cells [cells type]
  (for [[x y] cells]
    [:rect {:x (* x size) :y (* y size) :height size :width size :class type}]))

(defn world [{:keys [food snake]} owner]
  (reify
    om/IRender
    (render [this]
      (html
        [:svg {:width (* width size) :height (* width size)}
         (render-cells food "food")
         (render-cells snake "snake")]))))

(defn next-head [[[x y]] direction]
  (condp = direction
    :left  [[(dec x) y]]
    :up    [[x (dec y)]]
    :right [[(inc x) y]]
    :down  [[x (inc y)]]))

(defn move [{:keys [snake direction] :as app}]
  (update-in app [:snake] next-head direction))

(defn root [app owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (let [keyboard-channel (listen js/window "keydown")]
        (go (loop [step-channel (timeout 200)]
              (alt!
                keyboard-channel ([e c]
                                    (when-let [direction (keycodes (.-keyCode e))]
                                      (om/update! app [:direction] direction))
                                    (recur (timeout 200)))
                step-channel ([e c]
                                    (om/transact! app move)
                                    (recur (timeout 200))))))))

    om/IRender
    (render [this]
      (html [:div
             [:h1 "Welcome to Snake !!"]
             [:div
              (str "Total food: " (count (:food app))
                   " Snake length: " (count (:snake app))
                   " Direction: " (:direction app))]
             (om/build world app)]))))

(om/root
  root
  app-state
  {:target (. js/document (getElementById "app"))})
