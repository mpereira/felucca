(ns battle-arena.geometry)

(defn point-distance [[p0x p0y] [p1x p1y]]
  (Math/sqrt (+ (Math.pow (- p0x p1x) 2) (Math.pow (- p0y p1y) 2))))

(defn point-in-triangle? [[px py] [[t0x t0y] [t1x t1y] [t2x t2y]]]
  (let [n (* 0.5
             (+ (* (- t1y) t2x)
                (* t0y (+ (- t1x) t2x))
                (* t0x (- t1y t2y))
                (* t1x t2y)))
        sign (if (neg? n) -1 1)
        s (* sign
             (+ (* t0y t2x)
                (- (* t0x t2y))
                (* (- t2y t0y) px)
                (* (- t0x t2x) py)))
        t (* sign
             (+ (* t0x t1y)
                (- (* t0y t1x))
                (* (- t0y t1y) px)
                (* (- t1x t0x) py)))]
    (and (>= s 0) (>= t 0) (<= (+ s t) (* 2 n sign)))))

(defn point-in-quadrilateral? [[px py] [qx qy qw qh]]
  (and (<= qx px) (< px (+ qx qw)) (<= qy py) (< py (+ qy qh))))
