(ns felucca.utils
  (:use arcadia.core)
  (:require [felucca.vector3 :refer :all])
  (:import IEnumerator
           Timeline
           [UnityEngine
            Application
            Camera
            CharacterController
            Debug
            KeyCode
            Mathf
            Physics
            Renderer
            Ray
            RaycastHit
            Time]))

(defn ->seq [ary]
  (let [e (.GetEnumerator ary)]
    (loop [coll '()]
      (if (.MoveNext e)
        (recur (cons (.Current e) coll))
        coll))))

(defn find-object [s] (. GameObject (Find s)))

(defn main-camera [] (UnityEngine.Camera/main))

(defn height [object]
  (with-cmpt object [r Renderer]
    (.. r bounds size y)))

(defn install-hooks [game-object role-key hooks]
  (doseq [[hook hook-fn] hooks]
    (hook+ game-object hook role-key hook-fn)))

(defn after [seconds f]
  (let [waited? (atom false)
        ^WaitForSeconds wait-for-seconds (WaitForSeconds. seconds)]
    (.. (Camera/main)
        (GetComponent Camera)
        (StartCoroutine (reify
                          IEnumerator
                          (MoveNext [this]
                            (let [continue? (not @waited?)]
                              (when @waited? (f))
                              (reset! waited? true)
                              continue?))
                          (get_Current [this]
                            wait-for-seconds))))))
