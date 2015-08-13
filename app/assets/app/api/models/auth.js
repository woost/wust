angular.module("wust.api").service("Auth", Auth);

Auth.$inject = ["$rootScope", "$window", "restmod", "jwtHelper", "store", "Session"];

function Auth($rootScope, $window, restmod, jwtHelper, store, Session) {
    let self = this;

    let authStore = store.getNamespacedStore("auth");
    let userKey = "currentUser";
    let service = {
        signup: restmod.model("/auth/signup"),
        signin: restmod.model("/auth/signin"),
        signout: restmod.singleton("/auth/signout")
    };

    this.login = _.partial(authenticate, service.signin, "Logged in");
    this.register = _.partial(authenticate, service.signup, "Registered");
    this.logout = logout;
    this.current = authStore.get(userKey) || {};
    this.checkLoggedIn = checkLoggedIn;

    checkLoggedIn();
    if (self.current.token)
        Session.update();

    // every time the window gets focused, clear the inMemoryCache of the store,
    // so all changes in store get propagated into our current angular session.
    // if the value has changed, we trigger rerendering of the whole.
    // this is only needed if storage is available, otherwise we won't share data between buffers.
    if (authStore.storageAvailable) {
        $window.onfocus = () => {
            let prev = authStore.get(userKey);
            delete authStore.inMemoryCache[userKey];
            let response = authStore.get(userKey) || {};
            self.current.identifier = response.identifier;
            self.current.token = response.token;
            self.current.userId = response.userId;
            $rootScope.$apply();
        };
    }

    function checkLoggedIn() {
        if (self.current.token && jwtHelper.isTokenExpired(self.current.token))
            logoutLocally();
    }

    function authenticate(model, message, user) {
        model.$create(user).$then(response => {
            self.current.identifier = response.identifier;
            self.current.token = response.token;
            self.current.userId = response.userId;
            authStore.set(userKey, self.current);
            Session.update();
            humane.success(message);
        });
    }

    function logout() {
        // TODO: should this really be a get request
        // also it does not do anything, except telling the server "i clicked logout" - who cares? we just forget the jwt token locally.
        service.signout.$fetch().$then(response => {
            logoutLocally();
            humane.success("Logged out");
        }, logoutLocally);
    }

    function logoutLocally() {
        delete self.current.identifier;
        delete self.current.token;
        delete self.current.userId;
        authStore.remove(userKey);
        Session.forget();
    }
}
