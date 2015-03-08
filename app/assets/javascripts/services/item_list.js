app.service('ItemList', function($stateParams, Graph) {
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
            var elem = container.list[$index];
            removeFunc(elem.id).$promise.then(function(data) {
                toastr.success("Removed Item");
                container.list.splice($index, 1);
            });
        };
    }

    function Item(queryFunc, createFunc) {
        queryFunc = queryFunc || empty;

        this.list = queryFunc($stateParams.id);
        this.new = {
            title: ""
        };
        this.add = createFunc ? add(createFunc, this) : todo;
        this.remove = remove(Graph.remove, this);
    }
});
