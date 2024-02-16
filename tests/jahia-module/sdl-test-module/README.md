# JContent Test Module


## Html5 Plugin Test Component

| Definitions                     | Use Case                                |
|---------------------------------|-----------------------------------------|
| cent:html5PluginTestComponent   | Use to manually test the HTML5 plugin   |

- Add the component **html5PluginTestComponent** to a page
- Validate that the video selection is working

### Structure of the HTML5 Plugin test component
 The folder `tests/jahia-module/jcontent-test-module/src/main/resources/javascript/ckeditor` contains the following files:
 - `html5video` plugin folder
 - `widgetselection` plugin folder

Those have been downloaded from https://ckeditor.com/cke4/addon/html5video

The file `tests/jahia-module/jcontent-test-module/src/main/resources/javascript/config.js` contains the following code , to declare the plugins:
```javascript
CKEDITOR.editorConfig = function( config ) {
    config.extraPlugins = 'html5video,widgetselection';
    config.allowedContent = true;
    config.html5video = {
        // Define the default video width
        defaultVideoWidth: 640,
        // Define the default video height
        defaultVideoHeight: 480
    };
};
```
It is then added in the definition as is:
```cnd
[cent:html5PluginTestComponent] > jnt:content, mix:title, jmix:editorialContent, jmix:droppableContent, jmix:siteComponent
 - text (string, richtext[ckeditor.customConfig='$context/modules/jcontent-test-module/javascript/config.js'])
```
