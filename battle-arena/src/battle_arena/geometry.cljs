(ns battle-arena.geometry)

(defn point-distance [[p0x p0y] [p1x p1y]]
  (Math/sqrt (+ (Math.pow (Math.abs (- p0x p1x)) 2)
                (Math.pow (Math.abs (- p0y p1y)) 2))))

(defn point-in-triangle? [[px py] [[p0x p0y] [p1x p1y] [p2x p2y]]]
  (let [n (* 0.5
             (+ (* (- p1y) p2x)
                (* p0y (+ (- p1x) p2x))
                (* p0x (- p1y p2y))
                (* p1x p2y)))
        sign (if (neg? n) -1 1)
        s (* sign
             (+ (* p0y p2x)
                (- (* p0x p2y))
                (* (- p2y p0y) px)
                (* (- p0x p2x) py)))
        t (* sign
             (+ (* p0x p1y)
                (- (* p0y p1x))
                (* (- p0y p1y) px)
                (* (- p1x p0x) py)))]
    (and (>= s 0) (>= t 0) (<= (+ s t) (* 2 n sign)))))
