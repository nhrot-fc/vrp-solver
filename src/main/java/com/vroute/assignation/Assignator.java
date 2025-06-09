package com.vroute.assignation;

import com.vroute.models.Environment;

public interface Assignator {
    Solution solve(Environment env);
}
