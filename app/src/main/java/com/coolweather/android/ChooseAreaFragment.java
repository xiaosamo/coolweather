package com.coolweather.android;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by yuan on 2017/10/26.
 */

public class ChooseAreaFragment extends Fragment
{
    //3种状态
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    //进度对话框
    private ProgressDialog progressDialog;

    //标题
    private TextView titleText;

    //返回按钮
    private Button backButton;

    //listView
    private ListView listView;

    //适配器
    private ArrayAdapter<String> adapter;

    //当前选中的数据列表
    private List<String> dataList = new ArrayList<>();

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City> cityList;

    //县列表
    private List<County> countyList;

    //选中的省份
    private Province selectedProvince;

    //选中的城市
    private City selectedCity;

    //当前选中的级别
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        //listView添加点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (currentLevel == LEVEL_PROVINCE)
                {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        //返回按钮添加点击事件
        backButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (currentLevel == LEVEL_COUNTY)
                {
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    queryProvinces();
                }
            }
        });
        //先调用省的数据
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再到服务器上查询
     */
    private void queryProvinces()
    {
        titleText.setText("中国");
        //设置返回按钮不可见
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0)
        {
            dataList.clear();
            for (Province province : provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }
        else
        {
            String address = "http://guolin.tech/api/china";
            queryFromSever(address, "province");
        }
    }


    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再到服务器上查询
     */
    private void queryCities()
    {
        //显示省的名字
        titleText.setText(selectedProvince.getProvinceName());
        //设置返回按钮可见
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0)
        {
            dataList.clear();
            for (City city : cityList)
            {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromSever(address, "city");
        }
    }


    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再到服务器上查询
     */
    private void queryCounties()
    {
        //显示当前选择的城市名称
        titleText.setText(selectedCity.getCityName());
        //设置返回按钮可见
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0)
        {
            dataList.clear();
            for (County county : countyList)
            {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromSever(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void queryFromSever(String address, final String type)
    {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback()
        {

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type))
                {
                    result = Utility.handleProvinceResponse(responseText);
                }
                else if ("city".equals(type))
                {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                }
                else if ("county".equals(type))
                {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                if (result)
                {
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            closeProgressDialog();
                            if ("province".equals(type))
                            {
                                queryProvinces();
                            }
                            else if ("city".equals(type))
                            {
                                queryCities();
                            }
                            else if ("county".equals(type))
                            {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e)
            {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        closeProgressDialog();

                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog()
    {
        if (progressDialog == null)
        {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog()
    {
        if (progressDialog != null)
        {
            progressDialog.dismiss();
        }
    }

}
