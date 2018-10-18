The OSMRec repository is now under [SLIPO-EU](https://github.com/SLIPO-EU/OSMRec) project.

# OSMRecPlugin
___
##

## Info 

The purpose of the plugin is to recommend already existing categories/tags on newly created OSM entities.
When a user creates a new object on the map, the plugin provides a list of the most relevant existing categories (expressed as tags). 
The tags can be added automatically to the selected OSM entity.

The classification process is carried out using Support Vector Machines by analysing spatial entities into training features. 
The recommended tags are provided by the produced SVM model from the training process.

OSMRec is implemented as a  [JOSM plugin] [JOSM plugin], thus allowing the real-time, accurate and consistent categorization of newly created spatial entities in OpenStreetMap, through JOSM interface.

OSMRec plugin can be [downloaded] [plugins] and installed in JOSM following the [standard procedure].

## Usage

1. Copy and paste OSMRecPlugin.jar file in the plugins directory under the JOSM directory.
 * On a **Linux** system you will find the JOSM directory in the user directory `/home/username/.josm/plugins`. The ".josm" directory is hidden, so hit `Ctrl+H` to display hidden files. 
 * Under **Windows**, the plugins are located in `C:\Users\username\AppData\Roaming\JOSM\plugins` or `C:\Documents and Settings\username\Application Data\JOSM\plugins`.

2. Restart JOSM and OSMRec should show up in the plugin list under *Edit -> Preferences -> Plugins*. 
Activate the Plugin, click OK to save the preferences and restart JOSM once more. 

3. After you have loaded a map in JOSM, click from the menu bar *Tools -> OSM Recommendation*. 
A toggle box appears at the right side, under the rest of the toggle boxes. 
It provides two facilities: (a) training recommendation models and (b) obtaining category recommendations for entities. 

#### Training
For training recommendation models, after clicking the “Train a Model” button, the training
configuration for the SVM model pops up. Here input: 
 * OSM file on which the model will be trained (default is the opened file in JOSM).
 * SVM parameters: 
    * If C is not provided, the system runs a cross-validation process against a set of different c parameters and evaluates
      the best C.
    * Maximum thresholds for the textual training features (top-k or max-Frequency). 

 * You can train a model by the editing history of a specific user, by area or by history, providing the additional info. 
 * Click Accept and Train Model button to start the training process.

Alternatively, you can use the already produced SVM models from the SVMModels folder. The folder contains models for several cities of the world. After you download the compressed file for your preferred city, simply extract the "OSMRec_models" folder in the same folder of the OSM file you are going to edit with JOSM. Now you are ready to get recommendations from OSMRec without having to train a model! 

#### Recommending
For the category recommendation process on newly inserted spatial entities, click the "Add Recommendation" button from the initial toggle box. 
The system loads the appropriate recommendation SVM model  and a top-10 list of recommended categories appear.

OSMRec allows to choose a custom model or combine different training models. 
By clicking the “Model Settings” button you can choose to use a single model or combine several SVM models with configurable weights.

## Building
Set the environment and build JOSM following these instructions: 
https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins

Then, download OSMRecPlugin project and put it inside the ~/josm/plugins directory. 
Open the project in your favourite IDE. The project should appear as a freeform java project.

#OSMRecCLI
___
##
## Usage

* Train a model: <br />
`java -jar OSMRecCLI.jar -train -i inputFile [-c confParameter -m model]`

*inputfile*: OSM file to be used as training set. <br />
*confParameter*: SVM configuration parameter 
(if not defined, the system will run with several c's and will use the best). <br />
*model*: optional filename for the SVM model that will be produced. <br />

* Get recommendations: <br />
`java -jar OSMRecCLI.jar -test -i inputfile [-m model -o outputFile]`

*inputfile*: OSM file to be used as test set. Recommendations will be predicted for every instance in this file. <br />
*model*: optional filename for the SVM model to be used for recommending. <br />
*outputfile*: optional filename for the file containing the category predictions. <br />

## Building

The project can be built with the standard procedure of any maven project.
The missing artifacts are provided inside the libs folder of the project for convenience.

# Licensing
The OSMRec software is provided as free software. It can be redistributed and/or modified under the terms
of the GNU General Public License as published by the Free Software Foundation; either version 3.0 of the
License, or (optionally) any later version.

[JOSM plugin]:http://wiki.openstreetmap.org/wiki/JOSM/Plugins/OSMRec
[plugins]:https://josm.openstreetmap.de/wiki/Plugins
[standard procedure]:http://wiki.openstreetmap.org/wiki/JOSM/Plugins#Installation
