package com.vroute.assignation;

import com.vroute.models.Environment;

public interface Optimizer {
    Solution solve(Environment env);
}
