# TODO: ugh:
import sys, pprint
sys.path.extend(['.','..','py'])
import h2o, h2o_util

#########
# Config:
algos = ['example', 'kmeans', 'deeplearning', 'glm']


###########
# Utilities
pp = pprint.PrettyPrinter(indent=4)  # pretty printer for debugging

def validate_builder(builder):
    assert 'parameters' in builder and isinstance(builder['parameters'], list)
    parameters = builder['parameters']
    assert len(parameters) > 0
    parameter = parameters[0]
    h2o_util.assertKeysExist(parameter, '', ['name', 'label', 'help', 'required', 'type', 'default_value', 'actual_value', 'level', 'dependencies', 'values'])


def list_to_dict(l, key):
    result = {}
    for entry in l:
        k = entry[key]
        result[k] = entry
    return result

def validate_actual_parameters(input_parameters, actual_parameters, training_frame, validation_frame):
    actuals_dict = list_to_dict(actual_parameters, 'name')
    for k, v in input_parameters.iteritems():
        # TODO: skipping some stuff for now because they aren't serialized properly
        if k is 'response_column':
            continue

        expected = str(v)
        # Python says True; json says true
        assert k in actuals_dict, "Expected key " + k + " not found in actual parameters list."

        if actuals_dict[k]['type'] == 'boolean':
            expected = expected.lower()

        assert expected == actuals_dict[k]['actual_value'], "Parameter with name: " + k + " expected to have input value: " + str(expected) + ", instead has: " + str(actuals_dict[k]['actual_value'])
    # TODO: training_frame, validation_frame


################
# The test body:
################

a_node = h2o.H2O("127.0.0.1", 54321)

# TODO: remove die fast test case:
if False:
    import_result = a_node.import_files(path="/Users/rpeck/Source/h2o2/smalldata/logreg/prostate.csv")
    parse_result = a_node.parse(key=import_result['keys'][0]) # TODO: handle multiple files
    prostate_key = parse_result['frames'][0]['key']['name']

    a_node.build_model(algo='kmeans', training_frame=prostate_key, parameters={'K': 2 }, timeoutSecs=240)

    sys.exit()

models = a_node.models()
print 'Models: '
pp.pprint(models)

frames = a_node.frames()
print 'Frames: '
pp.pprint(frames)


####################################
# test model_builders collection GET
print 'Testing /ModelBuilders. . .'
model_builders = a_node.model_builders(timeoutSecs=240)

print 'ModelBuilders: '
pp.pprint(model_builders)

for algo in algos:
    assert algo in model_builders['model_builders'], "Failed to find algo: " + algo
    builder = model_builders['model_builders'][algo]
    validate_builder(builder)
    

####################################
# test model_builders individual GET
print 'Testing /ModelBuilders/{algo}. . .'
for algo in algos:
    model_builder = a_node.model_builders(algo=algo, timeoutSecs=240)
    assert algo in model_builder['model_builders'], "Failed to find algo: " + algo
    builder = model_builders['model_builders'][algo]
    validate_builder(builder)

####################################
# test model_metrics collection GET
print 'Testing /ModelMetrics. . .'
model_metrics = a_node.model_metrics(timeoutSecs=240)

print 'ModelMetrics: '
pp.pprint(model_metrics)

####################################
# test model_metrics individual GET
# TODO

################################################
# Import prostate.csv
import_result = a_node.import_files(path="/Users/rpeck/Source/h2o2/smalldata/logreg/prostate.csv")
parse_result = a_node.parse(key=import_result['keys'][0]) # TODO: handle multiple files
pp.pprint(parse_result)
prostate_key = parse_result['frames'][0]['key']['name']

################################################
# Import allyears2k_headers.zip
import_result = a_node.import_files(path="/Users/rpeck/Source/h2o2/smalldata/airlines/allyears2k_headers.zip")
parse_result = a_node.parse(key=import_result['keys'][0]) # TODO: handle multiple files
pp.pprint(parse_result)
airlines_key = parse_result['frames'][0]['key']['name']

####################
# Build KMeans model
model_builders = a_node.model_builders(timeoutSecs=240)
pp.pprint(model_builders)

kmeans_builder = a_node.model_builders(algo='kmeans', timeoutSecs=240)['model_builders']['kmeans']

kmeans_model_name = 'prostate_KMeans_1' # TODO: currently can't specify the target key

print 'About to build a KMeans model. . .'
kmeans_parameters = {'K': 2 }
jobs = a_node.build_model(algo='kmeans', destination_key=kmeans_model_name, training_frame=prostate_key, parameters=kmeans_parameters, timeoutSecs=240) # synchronous
print 'Done building KMeans model.'

#######################################
# Build DeepLearning model for Prostate
dl_prostate_model_name = 'prostate_DeepLearning_1'

print 'About to build a DeepLearning model. . .'
dl_prostate_1_parameters = {'classification': True, 'response_column': 'CAPSULE', 'hidden': "[10, 20, 10]" }
jobs = a_node.build_model(algo='deeplearning', destination_key=dl_prostate_model_name, training_frame=prostate_key, parameters=dl_prostate_1_parameters, timeoutSecs=240) # synchronous
print 'Done building DeepLearning model.'

models = a_node.models()

print 'After Model build: Models: '
pp.pprint(models)


# TODO: remove fail-early test
# print 'About to score. . .'
# mm = a_node.score(model=dl_prostate_model_name, frame=prostate_key)
# assert mm is not None, "Got a null result for scoring: " + dl_prostate_model_name + " on: " + prostate_key
# assert 'auc' in mm, "ModelMetrics for scoring: " + dl_prostate_model_name + " on: " + prostate_key + " does not contain an AUC."
# assert 'cm' in mm, "ModelMetrics for scoring: " + dl_prostate_model_name + " on: " + prostate_key + " does not contain a CM."
# print "ModelMetrics for scoring: " + dl_prostate_model_name + " on: " + prostate_key + ":  " + repr(mm)

#######################################
# Build DeepLearning model for Airlines
dl_airlines_model_name = 'airlines_DeepLearning_1'

print 'About to build a DeepLearning model. . .'
dl_airline_1_parameters = {'classification': True, 'response_column': 'IsDepDelayed' }
jobs = a_node.build_model(algo='deeplearning', destination_key=dl_airlines_model_name, training_frame=airlines_key, parameters=dl_airline_1_parameters, timeoutSecs=240) # synchronous
print 'Done building DeepLearning model.'

models = a_node.models()

print 'After Model build: Models: '
pp.pprint(models)

############################
# Check kmeans_model_name
found_kmeans = False;
kmeans_model = None
for model in models['models']:
    if model['key'] == kmeans_model_name:
        found_kmeans = True
        kmeans_model = model

assert found_kmeans, 'Did not find ' + kmeans_model_name + ' in the models list.'
validate_actual_parameters(kmeans_parameters, kmeans_model['parameters'], prostate_key, None)

###################################
# Check dl_prostate_model_name
found_dl = False;
dl_model = None
for model in models['models']:
    if model['key'] == dl_prostate_model_name:
        found_dl = True
        dl_model = model

assert found_dl, 'Did not find ' + dl_prostate_model_name + ' in the models list.'
validate_actual_parameters(dl_prostate_1_parameters, dl_model['parameters'], prostate_key, None)

mm = a_node.score(model=dl_prostate_model_name, frame=prostate_key)
assert mm is not None, "Got a null result for scoring: " + dl_prostate_model_name + " on: " + prostate_key
assert 'auc' in mm, "ModelMetrics for scoring: " + dl_prostate_model_name + " on: " + prostate_key + " does not contain an AUC."
assert 'cm' in mm, "ModelMetrics for scoring: " + dl_prostate_model_name + " on: " + prostate_key + " does not contain a CM."
print "ModelMetrics for scoring: " + dl_prostate_model_name + " on: " + prostate_key + ":  " + repr(mm)

###################################
# Check dl_airlines_model_name
found_dl = False;
dl_model = None
for model in models['models']:
    if model['key'] == dl_airlines_model_name:
        found_dl = True
        dl_model = model

assert found_dl, 'Did not find ' + dl_airlines_model_name + ' in the models list.'
validate_actual_parameters(dl_airline_1_parameters, dl_model['parameters'], airlines_key, None)

######################################################################
# Now look for kmeans_model_name using the one-model API, and check it
model = a_node.models(key=kmeans_model_name, find_compatible_frames=True)
found_kmeans = False;
h2o_util.assertKeysExist(model['models'][0], '', ['compatible_frames'])
h2o_util.assertKeysExist(model['models'][0]['compatible_frames'], '', ['frames'])

found = False
for frame in model['models'][0]['compatible_frames']['frames']:
    if frame['key']['name'] == prostate_key:
        found = True
assert found, "Failed to find " + prostate_key + " in compatible_frames list."


###################
# test delete_model
a_node.delete_model(kmeans_model_name)
models = a_node.models()

found_kmeans = False;
for model in models['models']:
    if model['key'] == 'KMeansModel':
        found_kmeans = True

assert not found_kmeans, 'Found KMeansModel in the models list: ' + h2o_util.dump_json(models)

####################
# test delete_models
jobs = a_node.build_model(algo='kmeans', training_frame=prostate_key, parameters={'K': 2 }, timeoutSecs=240) # synchronous
a_node.delete_models()
models = a_node.models()

assert 'models' in models and 0 == len(models['models']), "Called delete_models and the models list isn't empty: " + h2o_util.dump_json(models)
