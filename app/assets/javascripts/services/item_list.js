app.service('ItemList', function($stateParams) {
    this.Item = Item;

    function empty() {
        return [];
    }

    function todo() {
        toastr.error("not implemented");
    }

    function add(createFunc, container) {
        return function() {
            createFunc($stateParams.id, container.new).$promise.then(function(data) {
                toastr.success("Added new item");
                container.list.push(data);
                container.new.title = "";
            });
        };
    }

    function remove(removeFunc, container) {
        return function($index) {
            removeFunc($stateParams.id, container.new).$promise.then(function(data) {
                toastr.success("Removed Item");
                container.list.splice($index, 1);
            });
        };
    }

    function Item(type, queryFunc, createFunc, removeFunc) {
        queryFunc = queryFunc || empty;
        createFunc = createFunc || todo;
        removeFunc = removeFunc || todo;

        this.type = type;
        this.list = queryFunc($stateParams.id);
        this.new = {
            title: ""
        };
        this.add = add(createFunc, this);
        this.remove = remove(removeFunc, this);
    }
});
