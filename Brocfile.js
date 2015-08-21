var compileSass = require('broccoli-sass');
var esTranspiler = require('broccoli-babel-transpiler');
var mergeTrees = require('broccoli-merge-trees');
var compileSass = require('broccoli-compass');
var funnel = require('broccoli-funnel');

var stylesTree = mergeTrees([
        funnel('app/assets/stylesheets', { include: ['*.scss']}),
        funnel('app/assets/app', { include: ['**/*.scss'] })
                                                  ]);

var styles = compileSass(stylesTree, {
  outputStyle: 'expanded',
  sassDir: '.',
})


var scripts = esTranspiler(funnel('app/assets/app', { include: ['**/*.js']}));

// Merge the compiled styles and scripts into one output directory.
module.exports = mergeTrees([styles, scripts]);
