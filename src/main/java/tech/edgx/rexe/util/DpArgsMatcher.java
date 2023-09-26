package tech.edgx.rexe.util;

import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

public class DpArgsMatcher implements ArgumentMatcher<Optional<Object[]>> {

    private Optional<Object[]> left;

    public DpArgsMatcher(Optional<Object[]> left) {
        this.left = left;
    }

    @Override
    public boolean matches(Optional<Object[]> right) {
        boolean match = true;
        if (left.isPresent() && right.isPresent()) {
            Iterator rIterator = Arrays.stream(right.get()).iterator();
            for (Object l : left.get()){
                if (! l.toString().equals(rIterator.next().toString())) {
                    match = false;
                }
            }
        } else {
            match = false;
        }
        return match;
    }
}