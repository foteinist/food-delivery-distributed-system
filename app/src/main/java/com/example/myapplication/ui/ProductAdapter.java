package com.example.myapplication.ui;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import java.util.*;

public class ProductAdapter extends BaseAdapter {
    Context context;
    ArrayList<MyObject> products;
    ArrayList<MyObject> cart;
    LayoutInflater inflater;
    boolean allowClick;
    private CartUpdateListener listener;



    public ProductAdapter(Context context, ArrayList<MyObject> products, ArrayList<MyObject> cart, boolean allowClick, CartUpdateListener listener) {
        this.context = context;
        this.products = products;
        this.cart = cart;
        this.inflater = LayoutInflater.from(context);
        this.allowClick = allowClick;
        this.listener = listener;

    }

    public ProductAdapter(Context context, ArrayList<MyObject> products, ArrayList<MyObject> cart, boolean allowClick) {
        this.context = context;
        this.products = products;
        this.cart = cart;
        this.inflater = LayoutInflater.from(context);
        this.allowClick = allowClick;
    }

    @Override
    public int getCount() {
        return products.size();
    }

    @Override
    public Object getItem(int position) {
        return products.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView name, price, quantityTextView;
        ImageView image;
        Button buttonAdd, buttonRemove;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_product, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.productName);
            holder.price = convertView.findViewById(R.id.productPrice);
            holder.image = convertView.findViewById(R.id.productImage);
            holder.buttonAdd = convertView.findViewById(R.id.buttonAdd);
            holder.buttonRemove = convertView.findViewById(R.id.buttonRemove);
            holder.quantityTextView = convertView.findViewById(R.id.quantityTextView);

            if (allowClick) {
                holder.buttonAdd.setVisibility(View.VISIBLE);
                holder.buttonRemove.setVisibility(View.VISIBLE);
            } else {
                holder.buttonAdd.setVisibility(View.GONE);
                holder.buttonRemove.setVisibility(View.GONE);
            }

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }



        MyObject product = products.get(position);

        holder.name.setText(product.getName());
        if (allowClick) {
            holder.price.setText("€" + product.getPrice());
        } else {
            double totalPrice = product.getQuantity() * product.getPrice();
            holder.price.setText("€" + String.format(Locale.getDefault(), "%.2f", totalPrice));
        }
        holder.quantityTextView.setText(String.valueOf(product.getQuantity()));

        if (allowClick) {
            holder.quantityTextView.setText(String.valueOf(product.getQuantity()));
        }
        else{
            holder.quantityTextView.setText("x" + product.getQuantity());

        }


        Glide.with(context).load(product.getImageUrl()).into(holder.image);

        holder.buttonAdd.setOnClickListener(v -> {
            int currentQuantity = product.getQuantity();
            product.setQuantity(currentQuantity + 1);
            holder.quantityTextView.setText(String.valueOf(product.getQuantity()));

            if (!cart.contains(product)) {
                cart.add(product);
            }
            notifyDataSetChanged();
            if (listener != null) {
                listener.onCartUpdated(cart);
            }
        });

        holder.buttonRemove.setOnClickListener(v -> {
            int currentQuantity = product.getQuantity();
            if (currentQuantity > 0) {
                product.setQuantity(currentQuantity - 1);
                holder.quantityTextView.setText(String.valueOf(product.getQuantity()));

                if (product.getQuantity() == 0) {
                    cart.remove(product);
                }
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onCartUpdated(cart);
                }
            }
        });

        return convertView;
    }
}
