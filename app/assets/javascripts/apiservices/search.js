angular.module("wust").factory("Search", function(restmod) {
    return restmod.model("/search").mix({
        all: { hasMany: "All" },
        goals: { hasMany: "Goal" },
        problems: { hasMany: "Problem" },
        ideas: { hasMany: "Idea" },
    }).$new();
});
