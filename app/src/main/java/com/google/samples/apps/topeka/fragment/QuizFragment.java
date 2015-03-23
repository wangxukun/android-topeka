/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.topeka.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.samples.apps.topeka.R;
import com.google.samples.apps.topeka.adapter.QuizPagerAdapter;
import com.google.samples.apps.topeka.adapter.ScoreAdapter;
import com.google.samples.apps.topeka.model.Category;
import com.google.samples.apps.topeka.persistence.TopekaDatabaseHelper;
import com.google.samples.apps.topeka.widget.NoTouchViewPager;
import com.google.samples.apps.topeka.widget.quiz.AbsQuizView;

/**
 * Encapsulates Quiz solving and displays it to the user.
 */
public class QuizFragment extends Fragment {

    /**
     * Interface definition for a callback to be invoked when the quiz is started.
     */
    public interface SolvedStateListener {

        /**
         * This method will be invoked when the category has been solved.
         */
        void onCategorySolved();
    }

    private static final String KEY_USER_INPUT = "USER_INPUT";
    private Category mCategory;
    private NoTouchViewPager mQuizView;
    private ScoreAdapter mScoreAdapter;
    private QuizPagerAdapter mQuizAdapter;
    private SolvedStateListener mSolvedStateListener;

    public static QuizFragment newInstance(String categoryId,
            SolvedStateListener solvedStateListener) {
        if (categoryId == null) {
            throw new IllegalArgumentException("The category can not be null");
        }
        Bundle args = new Bundle();
        args.putString(Category.TAG, categoryId);
        QuizFragment fragment = new QuizFragment();
        if (solvedStateListener != null) {
            fragment.mSolvedStateListener = solvedStateListener;
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String categoryId = getArguments().getString(Category.TAG);
        mCategory = TopekaDatabaseHelper.getCategoryWith(getActivity(), categoryId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mQuizView = (NoTouchViewPager) view.findViewById(R.id.quiz_pager);
        // TODO: 1/27/15 finalize animations
        decideOnViewToDisplay();
        super.onViewCreated(view, savedInstanceState);
    }

    private void decideOnViewToDisplay() {
        final boolean isSolved = mCategory.isSolved();
        if (isSolved) {
            showSummary();
            mSolvedStateListener.onCategorySolved();
        } else {
            mQuizView.setAdapter(getQuizAdapter());
            mQuizView.setCurrentItem(mCategory.getFirstUnsolvedQuizPosition() + 1, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        View currentView = mQuizView.getFocusedChild();
        if (currentView instanceof AbsQuizView) {
            outState.putBundle(KEY_USER_INPUT, ((AbsQuizView) currentView).getUserInput());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        restoreQuizState(savedInstanceState);
        super.onViewStateRestored(savedInstanceState);
    }

    private void restoreQuizState(final Bundle savedInstanceState) {
        if (null == savedInstanceState) {
            return;
        }
        mQuizView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft,
                    int oldTop, int oldRight, int oldBottom) {
                mQuizView.removeOnLayoutChangeListener(this);
                View currentChild = mQuizView.getChildAt(0);
                if (currentChild instanceof AbsQuizView) {
                    ((AbsQuizView) currentChild).setUserInput(savedInstanceState.
                            getBundle(KEY_USER_INPUT));
                }
            }
        });

    }

    private QuizPagerAdapter getQuizAdapter() {
        if (null == mQuizAdapter) {
            mQuizAdapter = new QuizPagerAdapter(getActivity(), mCategory);
        }
        return mQuizAdapter;
    }

    /**
     * Displays the next page.
     *
     * @return <code>true</code> if there's another quiz to solve, else <code>false</code>.
     */
    public boolean showNextPage() {
        if (null == mQuizView) {
            return false;
        }
        int nextItem = mQuizView.getCurrentItem() + 1;
        final int count = mQuizView.getAdapter().getCount();
        if (nextItem < count) {
            moveToNextItem(nextItem);
            return true;
        }
        markCategorySolved();
        return false;
    }

    private void moveToNextItem(int nextItem) {
        mQuizView.setCurrentItem(nextItem, true);
        TopekaDatabaseHelper.updateCategory(getActivity(), mCategory);
    }

    private void markCategorySolved() {
        mCategory.setSolved(true);
        TopekaDatabaseHelper.updateCategory(getActivity(), mCategory);
    }

    public void showSummary() {
        final ListView scorecardView = (ListView) getView().findViewById(R.id.scorecard);
        mScoreAdapter = getScoreAdapter();
        scorecardView.setAdapter(mScoreAdapter);
        scorecardView.setVisibility(View.VISIBLE);
        mQuizView.setVisibility(View.GONE);
    }

    private ScoreAdapter getScoreAdapter() {
        if (null == mScoreAdapter) {
            mScoreAdapter = new ScoreAdapter(mCategory);
        }
        return mScoreAdapter;
    }
}
