angular.module("wust.api").service("Session", Session);

Session.$inject = ["User", "Auth", "$q"];

function Session(User, Auth, $q) {
    this.history = (size) => userOnly(userId => User.$buildRaw({id:userId}).history.$search({size}).$asPromise());
    this.marks = {
        add: id => userOnly(userId => User.$buildRaw({id:userId}).marks.$buildRaw({id}).$save({}).$asPromise()),
        destroy: id => userOnly(userId => User.$buildRaw({id:userId}).marks.$buildRaw({id}).$destroy().$asPromise()),
        search: () => userOnly(userId => User.$buildRaw({id:userId}).marks.$search().$asPromise()),
    };

    function userOnly(handler) {
        if (Auth.current.userId)
            return handler(Auth.current.userId);

        let deferred = $q.defer();
        deferred.reject();
        return deferred.promise;
    }
}
