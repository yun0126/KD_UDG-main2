package com.example.gpx_test

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Button
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileInputStream



class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var selectFileButton: Button
    private lateinit var googleMap: GoogleMap
    private val READ_REQUEST_CODE = 42 //  파일을 읽어오기 위한 요청 코드



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 지도 프래그먼트를 가져와서 OnMapReadyCallback으로 등록
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        selectFileButton = findViewById(R.id.select_file_button) // 버튼을 레이아웃에서 참조
        selectFileButton.setOnClickListener { selectFile() } // 버튼 클릭 이벤트를 등록

    }
    // 지도가 준비되면 호출되는 콜백 함수
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }


    // 파일 선택을 위한 intent를 실행하는 함수
    private fun selectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE) // intent를 실행하고 결과를 받기 위한 startActivityForResult 함수를 호출
    }
    // 파일 선택 결과를 처리하는 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        googleMap.clear()       // 지도의 모든 내용 삭제.
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) { // 요청 코드와 결과 코드를 비교해서 선택된 파일이 있는 경우
            resultData?.data?.also { uri -> // 선택된 파일의 Uri 정보를 가져옴
                Log.d("SelectedFile", "Uri: $uri") // Uri 정보를 로그로 출력
                val file = getFileFromUri(uri) // Uri 정보를 사용하여 파일 객체를 만들어줌
                Log.d("SelectedFile", "Path: ${file?.absolutePath}") // 만들어진 파일의 절대 경로를 로그로 출력
                parseGpxFile(getFileFromUri(uri))
            }
        }
    }
    // Uri 정보를 사용하여 파일 객체를 만들어주는 함수
    @SuppressLint("Range")
    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = contentResolver ?: return null // contentResolver 객체를 가져오고 null이면 null을 반환

        val cursor = contentResolver.query(uri, null, null, null, null) // Uri 정보를 사용하여 Cursor를 가져옴
        cursor?.use {
            it.moveToFirst() // Cursor를 첫 번째 레코드로 이동
            val displayName = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)) // 파일 이름을 가져옴
            val size = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)) // 파일 크기를 가져옴
            val type = contentResolver.getType(uri) // 파일 타입을 가져옴

            val file = File(cacheDir, displayName) // 파일 이름을 사용하여 캐시 디렉토리에 파일 객체를 생성
            file.outputStream().use { output ->
                contentResolver.openInputStream(uri)?.use { input ->
                    input.copyTo(output) // 파일 내용을 복사하여 새로운 파일에 저장
                }
            }

            return file // 생성된 파일 객체를 반환
        }

        return null // Cursor가 null이면 null을 반환
    }

    private fun parseGpxFile(filePath: File?) {
        // XmlPullParser 객체 생성
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()

        // GPX 파일 열기
        val fileInputStream = FileInputStream(filePath)
        parser.setInput(fileInputStream, null)

        var eventType = parser.eventType
        var latitude = 0.0
        var longitude = 0.0
        var isTrkptTag = false

        // Polyline 객체 생성
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.RED)

        // 시작점과 끝점 좌표 초기화
        var startPoint: LatLng? = null
        var endPoint: LatLng? = null

        // GPX 파일 파싱
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                // 시작 태그를 만난 경우
                XmlPullParser.START_TAG -> {
                    // "trkpt" 태그를 만난 경우
                    if (parser.name == "trkpt") {
                        // "lat" 속성과 "lon" 속성의 값을 읽어와서 latitude와 longitude에 저장
                        isTrkptTag = true
                        latitude = parser.getAttributeValue(null, "lat").toDouble()
                        longitude = parser.getAttributeValue(null, "lon").toDouble()

                        // 첫 좌표를 시작점으로 저장
                        if (startPoint == null) {
                            startPoint = LatLng(latitude, longitude)
                        }
                    }
                }
                // 끝 태그를 만난 경우
                XmlPullParser.END_TAG -> {
                    // "trkpt" 태그를 만난 경우
                    if (parser.name == "trkpt") {
                        // Polyline 객체에 좌표 추가
                        isTrkptTag = false
                        val point = LatLng(latitude, longitude)
                        polylineOptions.add(LatLng(latitude, longitude))

                        // 마지막 좌표를 끝점으로 저장
                        endPoint = point
                    }
                }
            }
            // 다음 이벤트로 이동
            eventType = parser.next()
        }
        // Polyline을 지도에 추가
        googleMap.addPolyline(polylineOptions)
        // 시작점과 끝점에 마커 추가
        if (startPoint != null) {
            googleMap.addMarker(MarkerOptions().position(startPoint).title("StartPoint"))
        }

        if (endPoint != null) {
            googleMap.addMarker(MarkerOptions().position(endPoint).title("EndPoint"))
        }
        // 지도 카메라 위치 설정
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polylineOptions.points[0], 15f))
    }


}
