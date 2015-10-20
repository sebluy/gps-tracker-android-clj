(ns android.sebluy.gpstracker.remote.transitions-test
  (:require [clojure.test :as test]
            [android.sebluy.gpstracker.remote.transitions :as transitions]))

(test/deftest send-request-sent
  (let [state {:page :other-page}
        sent? true
        request :get-stuff]
    (test/is (= {:page :remote
                 :history '(:other-page)
                 :remote {:status :pending :request :get-stuff}}
                (transitions/send-request state sent? request)))))

(test/deftest send-request-not-sent
  (let [state {:page :other-page}
        sent? false
        request :get-stuff]
    (test/is (= {:page :remote
                 :history '(:other-page)
                 :remote {:status :disconnected :request :get-stuff}}
                (transitions/send-request state sent? request)))))
