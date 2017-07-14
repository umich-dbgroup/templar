#!/bin/bash

declare -a join_levels=("0" "1")
declare -a log_tmpl=("pred_proj" "pred" "comp_proj" "comp" "const_proj" "const")
declare -a log_amounts=("100" "1000" "10p" "25p" "50p")

for jl in "${join_levels[@]}"
do
    for lt in "${log_tmpl[@]}"
    do
        for lp in "${log_percent[@]}"
        do
            java -Xms64g -Xmx128g -cp build/libs/tb-nalir-all.jar edu.umich.tbnalir.SchemaAndLogTemplateGenerator data/sdss/schema/bestdr7 bestdr7_0.05.parsed $jl $lt $la random results/sdss_randomized/bestdr7_0.05.join${jl}.${lt}.${la}.err  2>&1 | tee results/sdss_randomized/bestdr7_0.05.join${jl}.${lt}.${la}.out
        done
    done 
done