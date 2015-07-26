angular.module("wust.api").service("Auth", Auth);

Auth.$inject = ["$rootScope", "$window", "restmod", "jwtHelper", "store"];

function Auth($rootScope, $window, restmod, jwtHelper, store) {
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
    this.loggedIn = loggedIn;
    this.current = authStore.get(userKey) || {};

    // every time the window gets focused, clear the inMemoryCache of the store,
    // so all changes in store get propagated into our current angular session.
    // if the value has changed, we trigger rerendering of the whole.
    $window.onfocus = () => {
        let prev = authStore.get(userKey);
        delete authStore.inMemoryCache[userKey];
        this.current = authStore.get(userKey) || {};
        if (prev !== this.current) {
            $rootScope.$apply();
        }
    };

    function loggedIn() {
        let currentUser = authStore.get(userKey);
        return currentUser && !jwtHelper.isTokenExpired(currentUser.token);
    }

    function authenticate(model, message, user) {
        model.$create(user).$then(response => {
            this.current = _.pick(response, "identifier", "token", "userId");
            authStore.set(userKey, this.current);
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
        authStore.remove(userKey);
        this.current = {};
    }
}
