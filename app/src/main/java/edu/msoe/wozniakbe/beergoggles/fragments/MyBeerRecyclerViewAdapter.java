package edu.msoe.wozniakbe.beergoggles.fragments;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import edu.msoe.wozniakbe.beergoggles.R;
import edu.msoe.wozniakbe.beergoggles.fragments.BeerListFragment.OnListFragmentInteractionListener;
import edu.msoe.wozniakbe.beergoggles.src.Beer;

import java.util.List;

/**
 * Author: Ben Wozniak (wozniakbe@msoe.edu)
 * Code generated with Android Studio list fragment wizard
 * {@link RecyclerView.Adapter} that can display a {@link Beer} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class MyBeerRecyclerViewAdapter extends RecyclerView.Adapter<MyBeerRecyclerViewAdapter.ViewHolder> {

    private final List<Beer> beers;
    private final OnListFragmentInteractionListener mListener;

    public MyBeerRecyclerViewAdapter(List<Beer> items, OnListFragmentInteractionListener listener) {
        beers = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_beer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        holder.beer = beers.get(position);
        holder.beerName.setText(beers.get(position).getName());
        holder.beerAbv.setText(beers.get(position).getAbv());
        if(beers.get(position).getIbu().equals("0")){
            holder.beerIbu.setText("?");
        } else {
            holder.beerIbu.setText(beers.get(position).getIbu());
        }


        holder.beerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.beer);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return beers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View beerView;
        public TextView beerName;
        public TextView beerAbv;
        public TextView beerIbu;
        public Beer beer;

        public ViewHolder(View view) {
            super(view);
            beerView = view;
            beerName = view.findViewById(R.id.name);
            beerAbv = view.findViewById(R.id.abv);
            beerIbu = view.findViewById(R.id.ibu);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + beerName.getText() + "'";
        }
    }
}
