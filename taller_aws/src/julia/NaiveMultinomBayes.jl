using TextAnalysis, MySQL

model = NaiveBayesClassifier([:profesional, :vacacion, :celebracion]);
fit!(model, "vete a la playa", :vacacion);
fit!(model, "mejor en la montaña", :vacacion);
fit!(model, "reuniones de trabajo", :profesional);
fit!(model, "presentación a cliente", :profesional);
fit!(model, "aniversario de boda", :celebracion);
fit!(model, "celebracion cumpleaños", :celebracion);

vocabulary = model.dict;
prior_word_prob =  model.weights ./ sum(model.weights, dims=1);
prior_class_prob = round.([1 / 3, 1 / 3, 1 / 3], digits=4);

# TODO: insertar en base de datos: las prob_class hay que hacerlo en función de los documentos reales cargados de cada clase.