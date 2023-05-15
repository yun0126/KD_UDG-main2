package com.joljak.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

//여기서 합칠거
//kdu udg main main
// 수정한 동환
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var isTracking = false                          // 트래킹
    private var isPaused = false                            // 일시정지
    private lateinit var mMap: GoogleMap
    private val REQUEST_CODE_LOCATION_PERMISSION = 1
    private val locationHistory = mutableListOf<LatLng>()   // 위치 저보 기록.
    private val READ_REQUEST_CODE = 42 //  파일을 읽어오기 위한 요청 코드

    private lateinit var selectFileButton: Button

    private var lastLocationUpdateTime: Long = 0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLocationUpdateTime > 5000) {  // 5초마다 업데이트
                Log.d("위치정보 요청", "위치정보:lat=${location.latitude}, lng=${location.longitude}")
                Log.d("isTracking상태", "isTracking: ${isTracking}")       // 위치갱신 상태 출력
                val latLng = LatLng(location.latitude, location.longitude)

                locationHistory.add(latLng)

                val polylineOptions = PolylineOptions()
                polylineOptions.color(Color.YELLOW)
                polylineOptions.width(5f)
                polylineOptions.addAll(locationHistory)
                mMap.addPolyline(polylineOptions)

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                lastLocationUpdateTime = currentTime
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 지도 프래그먼트를 가져와서 OnMapReadyCallback으로 등록
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        requestPermission()
        checkWriteExternalStoragePermission(this, 1000)     //파일저장 권한필요.
        Log.d("isTracking상태", "isTracking: ${isTracking}")

        // start/stop tracking 버튼 클릭 리스너 설정
        val btnTrack = findViewById<Button>(R.id.btn_track)
        btnTrack.setOnClickListener {
            if (isTracking) {
                stopTracking()

            } else {
                startTracking()
            }
        }
        // start/stop tracking 버튼 클릭 리스너 설정
        val btnPause = findViewById<Button>(R.id.btn_pause)         //일시정지 버튼
        btnPause.setOnClickListener {
            Log.d("일시정지클릭", "btnPauseClick")         // 버튼 클릭 확인
            if (!isPaused) {
//                stopTracking()
                stopOn()

                Log.d("stopOnClick", "isPaused: ${isPaused}") // 정지상태 출력
                Log.d("stopOnClick", "isTracking: ${isTracking}") // 트래킹상태 출력

            } else {
//                startTracking()
                stopOff()
                Log.d("stopOnClick", "isPaused: ${isPaused}") // 정지상태 출력
                Log.d("stopOnClick", "isTracking: ${isTracking}") // 트래킹상태 출력

            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                5f,
                locationListener
            )
        }

        // 권한이 허용되어 있는 경우
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            sensorManager.registerListener(
                rotationEventListener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        // 권한이 허용되어 있지 않은 경우
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }

        selectFileButton = findViewById(R.id.select_file_button) // 버튼을 레이아웃에서 참조
        selectFileButton.setOnClickListener {
            Log.d("selectFile 클릭.", "selectFile")
            selectFile()
        } // 버튼 클릭 이벤트를 등록


    }

    private val rotationEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // 센서 값이 변경될 때마다 호출되는 콜백 함수
            // 방향 정보를 업데이트 하는 코드 작성
//            Log.d("LocationHistory", locationHistory.toString()) // locationHistory 출력        너무 많이 나옴.

            if (event == null || !::mMap.isInitialized) return

            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(16)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                mMap.setPadding(0, 0, 0, 0)
                mMap.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder()
                            .target(mMap.cameraPosition.target)
                            .zoom(mMap.cameraPosition.zoom)
                            .bearing(azimuth)
                            .build()
                    )
                )

            }

        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // 센서 정확도가 변경될 때마다 호출되는 콜백 함수
            // 필요한 경우 구현
        }
    }

    private fun showFilenameInputDialog() {     // STOP TRACKING 클릭시 파일 저장 팝업.
        // 다이얼로그 빌더 객체 생성
        val builder = AlertDialog.Builder(this)
            .setTitle("File Name")

        // 뷰 설정
        val view = layoutInflater.inflate(R.layout.file_gpx_name, null)
        builder.setView(view)

        // 다이얼로그 버튼 클릭 리스너 설정
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            // 입력된 파일 이름 가져오기
            val fileName = view.findViewById<EditText>(R.id.file_name).text.toString()
            // 파일 이름이 비어있지 않으면 파일 저장 함수 호출
            if (fileName.isNotEmpty()) {        // 파일저장 정상처리.
                Log.d("파일저장","파일저장: ${fileName}")
                saveLocationHistoryAsGpxFile(fileName)
                isTracking = false
                mMap.clear()
                finLocationUpdates()
                updateButton()
            } else {
                Toast.makeText(this, "파일 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        // 다이얼로그 띄우기
        val dialog = builder.create()
        dialog.show()
    }
    private fun saveLocationHistoryAsGpxFile(fileName: String) {       // gpx 파일 생성.

// 다운로드 폴더 경로
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val gpxFile = File(downloadDir, "$fileName.gpx")

//        val gpxFile = File(applicationContext.cacheDir, "$fileName.gpx")        //내부저장서
//        val writer = XmlWriter.create(openFileOutput(gpxFile,"MODE_PRIVATE"),"UTF-8")


//        val gpxFile = File(getExternalFilesDir(null), "$fileName.gpx")                //외부저장소
        Log.d("Saved file path", gpxFile.path)
        Toast.makeText(this, "파일 저장 위치: ${gpxFile.path}", Toast.LENGTH_LONG).show()

        if (gpxFile.exists()) {
            Toast.makeText(this, "동일한 이름의 파일이 이미 존재합니다.", Toast.LENGTH_SHORT).show()
            return
        }


        try {
//            val writer = XmlWriter.create(openFileOutput(gpxFile),"UTF-8")

            val writer = XmlWriter.create(FileOutputStream(gpxFile),"UTF-8")
            writer.startDocument("UTF-8")
//            writer.setPrefix("", "http://www.topografix.com/GPX/1/1")     //test
            writer.startTag("","gpx xmlns=\"http://www.topografix.com/GPX/1/1\"" )
            writer.attribute("", "creator", "bike")

            writer.attribute("", "version", "1.1")
            writer.attribute("", "xmlns:xsd", "http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance")
            writer.startTag("", "metadata")
            writer.startTag("", "name")
            writer.text("$fileName")
            writer.endTag("", "name")
            writer.startTag("", "link")
            writer.attribute("", "href", "http://www.bike.kr")
            writer.endTag("", "link")
            writer.startTag("", "desc")
            writer.text("giljabi")
            writer.endTag("", "desc")
            writer.startTag("", "copyright")
            writer.text("giljabi")
            writer.endTag("", "copyright")
            writer.startTag("", "speed")
            writer.text("15")
            writer.endTag("", "speed")
            writer.newline()
            writer.endTag("", "metadata")

            writer.startTag("", "trk")

            writer.startTag("", "trkseg")
            for (location in locationHistory) {
                writer.startTag("", "trkpt")
                writer.attribute("", "lat", location.latitude.toString())
                writer.attribute("", "lon", location.longitude.toString())
                writer.startTag("", "ele")
                writer.text("0")
                writer.endTag("", "ele")
                writer.newline()
                writer.endTag("", "trkpt")
            }
            writer.newline()
            writer.endTag("", "trkseg")
            writer.newline()
            writer.endTag("", "trk")
            writer.newline()
            writer.endTag("", "gpx")
            writer.endDocument()
            writer.close()

            Toast.makeText(this, "$fileName.gpx 파일 저장 완료", Toast.LENGTH_SHORT).show()

        } catch (e: IOException) {
            Toast.makeText(this, "파일 저장 실패", Toast.LENGTH_SHORT).show()
            Log.e("MyTracks", "파일 저장 실패", e)
        }
    }


    private fun checkWriteExternalStoragePermission(activity: Activity, requestCode: Int): Boolean {        // gpx파일 생성, 저장 권한

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val granted = PackageManager.PERMISSION_GRANTED
        return if (ContextCompat.checkSelfPermission(activity, permission) != granted) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
            false
        } else {
            true
        }
    }

    // 파일 선택을 위한 intent를 실행하는 함수
    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT ).apply {        //내부 저장소에 접근
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {        //파일 시스템의 모든 문서
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE) // intent를 실행하고 결과를 받기 위한 startActivityForResult 함수를 호출
    }

    // 파일 선택 결과를 처리하는 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        mMap.clear()       // 지도의 모든 내용 삭제.
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

        // Polyline 객체 생성
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.RED)

        // 시작점과 끝점 좌표 초기화
//        latLng 사용
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
        mMap.addPolyline(polylineOptions)
        // 시작점과 끝점에 마커 추가
        if (startPoint != null) {
            mMap.addMarker(MarkerOptions().position(startPoint).title("StartPoint"))
        }

        if (endPoint != null) {
            mMap.addMarker(MarkerOptions().position(endPoint).title("EndPoint"))
        }
        // 지도 카메라 위치 설정
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polylineOptions.points[0], 15f))
    }



    private fun startTracking() {
        Log.d("startTracking", "트래킹시작")
        isTracking = true
        updateButton()
        startLocationUpdates()
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.YELLOW)
        polylineOptions.width(5f)
        polylineOptions.addAll(locationHistory)
        mMap.addPolyline(polylineOptions)


    }

    private fun stopTracking() {
        showFilenameInputDialog()
        Log.d("트래킹중지버튼", "트래킹중지버튼")
//        isTracking = false
//        updateButton()
//        finLocationUpdates()


    }

    private fun stopOn() {
        if (isTracking) {

            isPaused = true
            updateStopButton()
//        startLocationUpdates()
            pauseTracking()
            Log.d("stopOn", "stopOn") // updateButton 정보를 로그로 출력

        }
    }

    private fun stopOff() {
        isPaused = false
        updateStopButton()
        resumeTracking()

        Log.d("stopOff", "stopOff") // updateButton 정보를 로그로 출력

    }

    private fun updateButton() {
        val btnTrack = findViewById<Button>(R.id.btn_track)
        btnTrack.text = if (isTracking) "Stop Tracking" else "Start Tracking"
        Log.d("updateButton", "isTracking: ${isTracking}") // updateButton 정보를 로그로 출력
    }

    private fun updateStopButton() {
        val btnPause = findViewById<Button>(R.id.btn_pause)
        btnPause.text = if (isPaused) "위치 갱신" else "일시정지"
        Log.d("updateStopButton", "isPaused: ${isPaused}") // updateStopButton 정보를 로그로 출력
    }

    override fun onDestroy() {
        super.onDestroy()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)

    }

    private fun requestPermission() {
        if (isPermissionGranted()) {
            // 권한이 있으면 onMapReady()에서 startLocationUpdates()를 호출하도록 변경
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_CODE_LOCATION_PERMISSION
        )
    }

    private fun isPermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 100
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (isPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isCompassEnabled = true
            mMap.uiSettings.isRotateGesturesEnabled = true

            mMap.setOnMyLocationChangeListener { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                mMap.setOnMyLocationChangeListener(null)
                startLocationUpdates()
            }
        } else {
            requestPermission()
        }
    }

    private fun startLocationUpdates() {
        if (mMap == null || mMap.myLocation == null) {
            return
        }

        val location = mMap.myLocation
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        Toast.makeText(this, "위치 업데이트를 시작합니다.", Toast.LENGTH_SHORT).show()

        if (isPermissionGranted()) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    locationListener

                )

            } catch (ex: SecurityException) {
                // 권한이 거부되는 경우 처리
                Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermission()
        }
    }


    private fun finLocationUpdates() {
        val locationManager = ContextCompat.getSystemService(this, LocationManager::class.java)
        locationManager?.removeUpdates(locationListener)

        locationHistory.clear()
        Toast.makeText(this, "위치 업데이트를 정지합니다.", Toast.LENGTH_SHORT).show()
    }
//    fun onPauseClick() {
//        if (!isTracking) {
//            return
//        }
//
//        if (isPaused) {
//            resumeTracking()
//        } else {
//            pauseTracking()
//        }
//    }

    override fun onResume() {
        super.onResume()
        if (isPermissionGranted()) {
            if (isTracking) {
                startLocationUpdates()
            }
        } else {
            requestPermission()
        }
    }

    private fun pauseTracking() {               // 위치 업데이트를 중지합니다.

        Log.d("pauseTracking", "stopOn") // updateButton 정보를 로그로 출력
        if (isTracking) {
            isTracking = false
            Toast.makeText(this, "pauseTracking", Toast.LENGTH_SHORT).show()
        }

        // 일시정지 상태를 나타내는 변수를 설정합니다.
        isPaused = true
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
    }


    private fun resumeTracking() {
        if (!isTracking) {

            startLocationUpdates()
            startTracking()
            isPaused = false
            Toast.makeText(this, "추적이 재개되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {                    // 재시작해도 현재 상태 유지.
        super.onSaveInstanceState(outState)
        // 번들 객체에 상태 정보 저장
        outState.putBoolean("isTracking", isTracking)
        outState.putParcelableArrayList("locationHistory", ArrayList(locationHistory))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "위치 권한을 허용 하지 않으면 이 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}