(load-file-macros "angular-cl2/src/angular.cl2")
(load-file "./sample-data.cl2")

(defapp myAppDev [myApp ngMockE2E])

(defservice simulator
  "Sets up some scenario for mocking design."
  [evaluation]
  (do-timeout 1000
              (reset! evaluation.opinions sample-opinions)))

(defmacro serve-entities
  "Serves entities from sample-data.cl2 via $httpBackend."
  [& entity-types]
  `(do
     ~@(apply
        concat
        (for [entity-type entity-types
              :let [sample-entities
                    (symbol (str "sample-" entity-type))]]
          (list
           ;; dirty hack so that mock data can be served independently
           ;; via similar urls
           `(let [ms (for [[id data] ~sample-entities
                           :let [url (+ "/api/" ~entity-type "/" id)]]
                       (assoc {} url [200 (serialize data)]))
                  routes (apply merge ms)]
              (doseq [[url response] routes]
                (.. $httpBackend
                  (whenGET url)
                  (respond
                   (fn [_ url]
                     (get routes url))))))

           `(.. $httpBackend
                (whenGET (+ "/api/" ~entity-type))
                (respond
                 (fn []
                   [200 (serialize ~sample-entities)])))
           )))))

(. myAppDev
   (run (fn-di [$httpBackend simulator]
               (serve-entities "boards"
                               "users"
                               "templates"
                               "projects"
                               "langs")
               (.. $httpBackend
                   (whenGET #"^partials/")
                   passThrough)
               )))
