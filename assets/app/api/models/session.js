angular.module("wust.api").service("Session", Session);

Session.$inject = ["User", "Auth", "$q", "$injector", "Post"];

function Session(User, Auth, $q, $injector, Post) {
    let self = this;
    this.addNotification = addNotification;
    this.loadNotifications = loadNotifications;
    this.history = (size) => userOnly(userId => User.$buildRaw({id:userId}).history.$search({size}).$asPromise());
    this.marks = {
        add: id => userOnly(userId => User.$buildRaw({id:userId}).marks.$buildRaw({id}).$save({}).$asPromise()),
        destroy: id => userOnly(userId => User.$buildRaw({id:userId}).marks.$buildRaw({id}).$destroy().$asPromise()),
        search: () => userOnly(userId => User.$buildRaw({id:userId}).marks.$search().$asPromise()),
    };

    this.notifications = [];
    loadNotifications();

    function addNotification(post) {
        //TODO: fix deps
        let currentComponent = $injector.get("HistoryService").getCurrentViewComponentGraph();
        if (currentComponent && _.any([currentComponent.rootNode].concat(currentComponent.rootNode.deepPredecessors), _.pick(post, "id"))) {
            //TODO: how to make the notifications go away
            //HACK: WORKAROUND: review the rootnode, then get the notifications again...
            Post.$buildRaw(_.pick(currentComponent.rootNode, "id")).view.$create().$then(loadNotifications);
        } else {
            //TODO: response should include which followed node was responsible for the notification, so we do not have to reload
            loadNotifications();
        }
    }

    function loadNotifications() {
        let promise = userOnly(userId => {
            return User.$buildRaw({id:userId}).notifications.$search().$asPromise();
        });
        promise.then(data => self.notifications = data.$asList());
        return promise;
    }

    function userOnly(handler) {
        if (Auth.current.userId)
            return handler(Auth.current.userId);

        let deferred = $q.defer();
        deferred.reject();
        return deferred.promise;
    }
}
